package net.onrc.openvirtex.migrator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.onrc.openvirtex.api.service.handlers.ApiHandler;
import net.onrc.openvirtex.api.service.handlers.HandlerUtils;
import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.api.service.handlers.tenant.ConnectHost;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.exceptions.DuplicateMACException;
import net.onrc.openvirtex.exceptions.InvalidPortException;
import net.onrc.openvirtex.exceptions.InvalidTenantIdException;
import net.onrc.openvirtex.exceptions.MissingRequiredField;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.exceptions.SwitchMappingException;
import net.onrc.openvirtex.routing.ShortestPath;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class MigrateVM extends ApiHandler<Map<String, Object>> {

	Logger log = LogManager.getLogger(MigrateVM.class.getName());
	private VSDNMigrator vsdnm = new VSDNMigrator();
	Host host;
	String message;

	PhysicalNetwork physicalNetwork = PhysicalNetwork.getInstance();
	OVXMap ovxMap = OVXMap.getInstance();

	@Override
	public JSONRPC2Response process(final Map<String, Object> params) {
		JSONRPC2Response resp = null;
		message = "failed";
		try {
			final Number tenantId = HandlerUtils.<Number> fetchField(
					TenantHandler.TENANT, params, true, null);
			final Number hid = HandlerUtils.<Number> fetchField(
					TenantHandler.VSDN_HOST_ID, params, true, null);
			final String hmac = HandlerUtils.<String> fetchField(
					TenantHandler.VSDN_HOST_MAC, params, true, null);
			final String spdpid = HandlerUtils
					.<String> fetchField(
							TenantHandler.VSDN_SWITCH_PHYSICAL_DPID, params,
							true, null);
			final Number pport = HandlerUtils.<Number> fetchField(
					TenantHandler.VSDN_PHYSICAL_PORT, params, true, null);

			//log.info("********* Got the python ********" + tenantId + " " + hid
			//		+ " " + hmac + " " + " " + spdpid + " " + pport);
			if (checkValidity(tenantId.intValue(), hid.intValue(), spdpid)) {
				vsdnm.openVirtualNetwork(tenantId.intValue());
				vsdnm.disconnectHost(hid.intValue());
				Long pdpid = Long.parseLong(spdpid.toString().replace(":", ""),
						16);
				PhysicalSwitch physicalSwitch = physicalNetwork
						.getSwitch(pdpid);

				List<OVXSwitch> virtualSwitches = new ArrayList<OVXSwitch>();
				virtualSwitches.addAll(ovxMap.getVirtualNetwork(
						tenantId.intValue()).getSwitches());

				List<PhysicalSwitch> physicalSwitches = new ArrayList<PhysicalSwitch>();
				for (OVXSwitch s : virtualSwitches) {
					physicalSwitches.addAll(ovxMap.getPhysicalSwitches(s));
				}

				/*
				 * Map<Short, PhysicalPort> m = physicalSwitch.getPorts(); for
				 * (Short key : m.keySet()) { PhysicalPort p = m.get(key);
				 * //log.info("Port info " + p.toString());
				 * 
				 * //TODO: Delete it, unused code
				 * 
				 * }
				 */

				if (!physicalSwitches.contains(physicalSwitch)) {
					OVXSwitch ovxSs, ovxDs;
					OVXPort ovxSp = null, ovxDp = null;
					ovxSs = vsdnm.createSwitch(tenantId.intValue(), pdpid);
					/*
					 * Old method, look for the new method below with the
					 * shortest path Map<Short, PhysicalPort> portMap =
					 * physicalSwitch .getPorts(); for (Short key :
					 * portMap.keySet()) { PhysicalPort p = portMap.get(key); if
					 * (!p.isEdge()) { ovxSp = vsdnm.createPort(pdpid,
					 * p.getPortNumber()); break; }
					 * 
					 * }
					 */
					ShortestPath sp = new ShortestPath();
					List<PhysicalLink> shortestPath = null;
					int shortestLength = 0;

					// Figure out the shortest path
					for (PhysicalSwitch ps : physicalSwitches) {
						List<PhysicalLink> path = sp.computePath(
								physicalSwitch, ps);
						int length = path.size();
						if (shortestPath == null) {
							shortestPath = path;
							shortestLength = length;
						} else if (shortestLength > length) {
							shortestPath = path;
							shortestLength = length;
						}
					}
					PhysicalLink src, dst;
					src = shortestPath.get(0);
					dst = shortestPath.get(shortestLength - 1);
					//log.info("()()()() " + shortestLength + " "
							//+ shortestPath.toString() + " \n "
							//+ src.getSrcPort() + src.getSrcSwitch()
						//	+ src.getDstPort() + src.getDstSwitch() + " "
						//	+ dst.getSrcPort() + dst.getSrcSwitch()
						//	+ dst.getDstPort() + dst.getDstSwitch());

					if (shortestLength == 1) {
						ovxSs = ovxMap.getVirtualSwitch(src.getSrcSwitch(),
								tenantId.intValue());
						ovxDs = ovxMap.getVirtualSwitch(src.getDstSwitch(),
								tenantId.intValue());
						ovxSp = ovxSs.getPort(src.getSrcPort().getPortNumber());
						ovxDp = ovxDs.getPort(src.getDstPort().getPortNumber());
						if (ovxSp == null) {
							ovxSp = vsdnm.createPort(src.getSrcSwitch()
									.getSwitchId(), src.getSrcPort()
									.getPortNumber());
						}
						if (ovxDp == null) {
							ovxDp = vsdnm.createPort(src.getDstSwitch()
									.getSwitchId(), src.getDstPort()
									.getPortNumber());

						}

					} else {
						ovxSs = ovxMap.getVirtualSwitch(src.getSrcSwitch(),
								tenantId.intValue());
						ovxDs = ovxMap.getVirtualSwitch(dst.getDstSwitch(),
								tenantId.intValue());
						ovxSp = ovxSs.getPort(src.getSrcPort().getPortNumber());
						ovxDp = ovxDs.getPort(dst.getDstPort().getPortNumber());
						if (ovxSp == null) {
							ovxSp = vsdnm.createPort(src.getSrcSwitch()
									.getSwitchId(), src.getSrcPort()
									.getPortNumber());
						}
						if (ovxDp == null) {
							ovxDp = vsdnm.createPort(dst.getDstSwitch()
									.getSwitchId(), dst.getDstPort()
									.getPortNumber());

						}
					}
					OVXLink vlink = vsdnm.createLink(ovxSs.getSwitchId(),
							ovxSp.getPortNumber(), ovxDs.getSwitchId(),
							ovxDp.getPortNumber(), "spf", Byte.parseByte("1"));
					if (vlink == null) {
						message = "Error Could not create Link";
					}

					//log.info("()()(yippe)()() \n" + ovxSs.toString() + " \n"
						//	+ ovxDs.toString() + " \n" + ovxSp.toString()
						//	+ " \n" + ovxDp.toString() + "\n "
						//	+ vlink.toString());

					/*
					 * if (ovxSp != null && ovxDp != null) {
					 * vsdnm.createLink(ovxSs.getSwitchId(),
					 * ovxSp.getPortNumber(), ovxDs.getSwitchId(),
					 * ovxDp.getPortNumber(), "spf", Byte.parseByte("1"));
					 * }else{ log.error(
					 * "Unable to create the virtual link for the new switch");
					 * }
					 */

				}
				/*TODO Cannot figure out newly attached hosts using this, have to figure out a new way
				Map<Short,PhysicalPort> m=physicalSwitch.getPorts();
				for(Short k:m.keySet()){
					PhysicalPort pp=m.get(k);
					if(pp.isEdge()){
						log.info("~~~~~~~~~~~~~~~~~~"+pp.);
					}
				}*/

				OVXPort ovxPort = vsdnm.createPort(
						Long.parseLong(spdpid.replace(":", ""), 16),
						pport.shortValue());
				OVXSwitch ovxs = ovxMap.getVirtualSwitch(physicalSwitch,
						tenantId.intValue());
				if (ovxPort != null) {
					host = vsdnm.connectHost(ovxs.getSwitchId(),
							ovxPort.getPortNumber(), hmac);

				}
				if (ovxPort != null && host != null) {
					message = "Successfylly migrated the host";
					//log.info("Successfylly migrated the host");
				} else {
					log.error("Some error occured, check Logs");
					message = "Some error occured, check Log";
				}
			} else {
				message = "Invalid host for migration";
			}
			Map<String, Object> reply = new HashMap<String, Object>();
			reply.put("message", message);
			if (host != null) {
				reply.put("host_id", host.getHostId());
			}
			resp = new JSONRPC2Response(reply, 0);

			/*
			 * HandlerUtils.isValidTenantId(tenantId.intValue()); //
			 * HandlerUtils.isValidOVXPort(tenantId.intValue(),
			 * dpid.longValue(), // port.shortValue());
			 * 
			 * final OVXMap map = OVXMap.getInstance(); final OVXNetwork
			 * virtualNetwork = map.getVirtualNetwork(tenantId .intValue());
			 * final MACAddress macAddr = MACAddress.valueOf(mac);
			 * HandlerUtils.isUniqueHostMAC(macAddr); final Host host =
			 * virtualNetwork.connectHost(dpid.longValue(), port.shortValue(),
			 * macAddr);
			 * 
			 * if (host == null) { resp = new JSONRPC2Response( new
			 * JSONRPC2Error( JSONRPC2Error.INTERNAL_ERROR.getCode(),
			 * this.cmdName()), 0); } else { this.//log.info(
			 * "Connected host with id {} and mac {} to virtual port {} on virtual switch {} in virtual network {}"
			 * , host.getHostId(), host.getMac().toString(), host
			 * .getPort().getPortNumber(), host.getPort()
			 * .getParentSwitch().getSwitchName(),
			 * virtualNetwork.getTenantId()); Map<String, Object> reply = new
			 * HashMap<String, Object>( host.getDBObject());
			 * reply.put(TenantHandler.TENANT, tenantId.intValue()); resp = new
			 * JSONRPC2Response(reply, 0); }
			 */
		} catch (final MissingRequiredField e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(
					JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
							+ ": Unable to connect host : " + e.getMessage()),
					0);
		} catch (final InvalidPortException e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(
					JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
							+ ": Invalid port : " + e.getMessage()), 0);
		} catch (final InvalidTenantIdException e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(
					JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
							+ ": Invalid tenant id : " + e.getMessage()), 0);
			/*
			 * ( } catch (final IndexOutOfBoundException e) { resp = new
			 * JSONRPC2Response( new JSONRPC2Error(
			 * JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName() +
			 * ": Impossible to create the virtual port, too many ports on this virtual switch : "
			 * + e.getMessage()), 0); } catch (final NetworkMappingException e)
			 * { resp = new JSONRPC2Response(new JSONRPC2Error(
			 * JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName() + ": " +
			 * e.getMessage()), 0);
			 */
		} catch (final DuplicateMACException e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(
					JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
							+ ": " + e.getMessage()), 0);
		} catch (SwitchMappingException e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(
					JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
							+ ": Invalid switch id " + e.getMessage()), 0);
		} catch (NetworkMappingException e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(
					JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
							+ ": Invalid tenant id " + e.getMessage()), 0);
		}

		return resp;
	}

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.OBJECT;
	}

	private boolean checkValidity(int tenantID, int hostID, String pdpid) {

		try {
			List<PhysicalSwitch> allSwitches = new ArrayList<PhysicalSwitch>();
			List<OVXSwitch> virtualSwitches = new ArrayList<OVXSwitch>();
			List<PhysicalSwitch> validSwitches = new ArrayList<PhysicalSwitch>();

			// Getting list of all physical switches
			allSwitches.addAll(physicalNetwork.getSwitches());
			// //log.info("^^^^^^^^^^ all switches : "+allSwitches.toString());

			// Getting all switches in the corresponding Virtual Network
			OVXNetwork ovxNetwork = ovxMap.getVirtualNetwork(tenantID);
			if (ovxNetwork.getTopologyRestriction()) {
				virtualSwitches.addAll(ovxNetwork.getSwitches());
				// //log.info("^^^^^^^^^^ all virtual switches : "+virtualSwitches.toString());
				Host host = ovxNetwork.getHost(hostID);
				OVXSwitch selfSwitch = host.getPort().getParentSwitch();
				// //log.info("^^^^^^^^^^ host switch : "+selfSwitch.toString());
				virtualSwitches.remove(selfSwitch);
				// //log.info("^^^^^^^^^^ all new virtual switches : "+virtualSwitches.toString());
				for (OVXSwitch s : virtualSwitches) {
					allSwitches.removeAll(ovxMap.getPhysicalSwitches(s));
				}
			}
			validSwitches.addAll(allSwitches);
			// //log.info("physical dpid in String : " + pdpid);

			long ndpid = Long.parseLong(pdpid.replace(":", ""), 16);
			// //log.info("physical dpid in long : " + ndpid);
			PhysicalSwitch newSwitch = physicalNetwork.getSwitch(ndpid);
			if (validSwitches.contains(newSwitch)) {
				return true;
			}

		} catch (NetworkMappingException e) {
			// TODO Auto-generated catch block
			log.error("Invalid tenant ID " + e.getMessage());
		} catch (SwitchMappingException e) {
			// TODO Auto-generated catch block
			log.error("Invalid switch dpid " + e.getMessage());
		}
		return false;
	}

}