package net.onrc.openvirtex.migrator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

import net.onrc.openvirtex.api.service.handlers.ApiHandler;
import net.onrc.openvirtex.api.service.handlers.HandlerUtils;
import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.api.service.handlers.tenant.ConnectHost;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.exceptions.DuplicateMACException;
import net.onrc.openvirtex.exceptions.InvalidPortException;
import net.onrc.openvirtex.exceptions.InvalidTenantIdException;
import net.onrc.openvirtex.exceptions.MissingRequiredField;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.exceptions.SwitchMappingException;

public class GetAllowedSwitches extends ApiHandler<Map<String, Object>> {

	Logger log = LogManager.getLogger(ConnectHost.class.getName());
	String message;

	@Override
	public JSONRPC2Response process(final Map<String, Object> params) {
		JSONRPC2Response resp = null;
		message = "failed";
		try {
			final Number tenantId = HandlerUtils.<Number> fetchField(
					TenantHandler.TENANT, params, true, null);
			final Number hid = HandlerUtils.<Number> fetchField(
					TenantHandler.VSDN_HOST_ID, params, true, null);

			log.info("********* Got the python ********" + tenantId + " " + hid);

			// Getting valid switches to migrate to

			List<PhysicalSwitch> allSwitches = new ArrayList<PhysicalSwitch>();
			List<OVXSwitch> virtualSwitches = new ArrayList<OVXSwitch>();
			List<PhysicalSwitch> validSwitches = new ArrayList<PhysicalSwitch>();

			// Getting list of all physical switches
			PhysicalNetwork physicalNetwork = PhysicalNetwork.getInstance();
			allSwitches.addAll(physicalNetwork.getSwitches());
			// log.info("^^^^^^^^^^ all switches : "+allSwitches.toString());

			// Getting all switches in the corresponding Virtual Network
			OVXMap ovxMap = OVXMap.getInstance();
			OVXNetwork ovxNetwork = ovxMap.getVirtualNetwork(tenantId
					.intValue());

			Host host = ovxNetwork.getHost(hid.intValue());
			if (host == null) {
				message = "failed : invalid host id";
			} else {
				if (ovxNetwork.getTopologyRestriction()) {
					virtualSwitches.addAll(ovxNetwork.getSwitches());
					// log.info("^^^^^^^^^^ all virtual switches : "+virtualSwitches.toString());
					OVXSwitch selfSwitch = host.getPort().getParentSwitch();
					// log.info("^^^^^^^^^^ host switch : "+selfSwitch.toString());
					virtualSwitches.remove(selfSwitch);
					// log.info("^^^^^^^^^^ all new virtual switches : "+virtualSwitches.toString());
					for (OVXSwitch s : virtualSwitches) {
						allSwitches.removeAll(ovxMap.getPhysicalSwitches(s));
					}

				}
				validSwitches.addAll(allSwitches);
				message = "success";
			}

			log.info("^^^^^^^^^^ valid physical switches : "
					+ validSwitches.toString());
			Map<String, Object> reply = new HashMap<String, Object>();
			reply.put("message", message);
			reply.put("valid_switches", validSwitches.toString());
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
			 * this.cmdName()), 0); } else { this.log.info(
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
		} catch (NetworkMappingException e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(
					JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
							+ ": Invalid tenant id : " + e.getMessage()), 0);
		} catch (SwitchMappingException e) {
			resp = new JSONRPC2Response(
					new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(),
							this.cmdName() + ": found an unmapped switch : "
									+ e.getMessage()), 0);
		}

		return resp;
	}

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.OBJECT;
	}

}