package net.onrc.openvirtex.migrator;

import java.util.ArrayList;
import java.util.List;

import net.onrc.openvirtex.api.service.handlers.HandlerUtils;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.exceptions.InvalidDPIDException;
import net.onrc.openvirtex.exceptions.InvalidHostException;
import net.onrc.openvirtex.exceptions.InvalidPortException;
import net.onrc.openvirtex.exceptions.InvalidTenantIdException;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.exceptions.PortMappingException;
import net.onrc.openvirtex.exceptions.SwitchMappingException;
import net.onrc.openvirtex.util.MACAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VSDNMigrator {

	private Logger log = LogManager.getLogger(VSDNMigrator.class.getName());
	private OVXMap map;
	private OVXNetwork virtualNetwork;
	private int tenantId;

	// This function opens an instance of the Virtual Network of a given tenant
	// ID
	public int openVirtualNetwork(int tenantId) {
		try {

			// check for valid tenant ID
			HandlerUtils.isValidTenantId(tenantId);

			// open the OVXMap to get the virtual network instance
			map = OVXMap.getInstance();
			virtualNetwork = map.getVirtualNetwork(tenantId);
			this.tenantId = tenantId;
		} catch (final NetworkMappingException e) {
			log.error("**** VSDNMigrator (ErrorCode : -3) **** : Network Mapping Exception in OpenVirtualNetwork()");
			return -3;
		}
		return 1;
	}

	public OVXSwitch createSwitch(int tenantId, long pdpid) {
		final List<Long> longDpids = new ArrayList<Long>();
		longDpids.add(pdpid);
		HandlerUtils.isValidDPID(tenantId, longDpids);
		OVXSwitch ovxSwitch = null;
		try {
			ovxSwitch = virtualNetwork.createSwitch(longDpids);
			if (ovxSwitch != null) {
				log.info("Successfull added a new switch with virtual dpid"
						+ ovxSwitch.getSwitchId() + " to the VirtualNetwork "
						+ tenantId);
			} else {
				log.error("Failed to add the switch with physical dpid "
						+ pdpid + " to the Virtual Network " + tenantId);
			}
		} catch (IndexOutOfBoundException e) {
			// TODO Auto-generated catch block
			log.error(e.getMessage());
		}
		return ovxSwitch;
	}

	public OVXLink createLink(long srcDpid, short srcPort, long dstDpid,
			short dstPort, String alg, byte backupNumber) {
		OVXLink virtualLink = null;

		try {
			HandlerUtils.isValidOVXSwitch(tenantId, srcDpid);
			HandlerUtils.isValidOVXSwitch(tenantId, dstDpid);
			HandlerUtils.isValidOVXPort(tenantId, srcDpid, srcPort);
			HandlerUtils.isValidOVXPort(tenantId, dstDpid, dstPort);
			virtualLink = virtualNetwork.connectLink(srcDpid, srcPort, dstDpid,
					dstPort, alg, backupNumber);
		} catch (PortMappingException | IndexOutOfBoundException e) {
			// TODO Auto-generated catch block
			log.error(e.getMessage());
		} catch (InvalidPortException e) {
			// TODO Auto-generated catch block
			log.error(e.getMessage());
		} catch (NetworkMappingException e) {
			// TODO Auto-generated catch block
			log.error(e.getMessage());
		}
		return virtualLink;
	}

	// This function disconnects the VM from its old switch and removes
	// corresponding mapping and flows
	public int disconnectHost(int hostId) {
		try {

			// verify the arguments
			HandlerUtils.isValidHostId(tenantId, hostId);

			// Disconnect the host
			virtualNetwork.disconnectHost(hostId);
			log.info("**** VSDNMigrator **** : Successfully removed the host");
		} catch (final InvalidTenantIdException e) {
			log.error("**** VSDNMigrator (ErrorCode : -1) **** : Invalid Tenant ID in disconnectHost()");
			return -1;
		} catch (final InvalidHostException e) {
			log.error("**** VSDNMigrator (ErrorCode : -2) **** : Invalid Host ID in disconnectHost()");
			return -2;
		} catch (final NetworkMappingException e) {
			log.error("**** VSDNMigrator (ErrorCode : -3) **** : Network Mapping Exception in disconnectHost()");
			return -3;
		}

		return 1;
	}

	// This Function creates a new virtual port in the Switch the VM is being
	// migrated to
	public OVXPort createPort(long pDpid, short pPort) {
		try {
			// verify the arguments
			HandlerUtils.isValidPhysicalPort(tenantId, pDpid, pPort);

			// Create New port
			OVXPort ovxPort = virtualNetwork.createPort(pDpid, pPort);

			// Check the vaildity of the new port
			if (ovxPort == null) {
				log.error("**** VSDNMigrator (ErrorCode : -999) **** : Inernal Error in createPort");
				return null;
			}
			log.info("**** VSDNMigrator **** : Successfully created a virtual port at port "
					+ pPort + " on switch " + pDpid);

			return ovxPort;

		} catch (final InvalidTenantIdException e) {
			log.error("**** VSDNMigrator (ErrorCode : -1) **** : Invalid Tenant ID in createPort");
			return null;
		} catch (final InvalidHostException e) {
			log.error("**** VSDNMigrator (ErrorCode : -2) **** : Invalid Host ID in createPort");
			return null;
		} catch (final SwitchMappingException e) {
			log.error("**** VSDNMigrator (ErrorCode : -4) **** : Switch Mapping Exception in createPort");
			return null;
		} catch (final InvalidPortException e) {
			log.error("**** VSDNMigrator (ErrorCode : -6) **** : Invalid Port Exception in createPort");
			return null;
		} catch (final InvalidDPIDException e) {
			log.error("**** VSDNMigrator (ErrorCode : -7) **** : Invalid dpid Exception in createPort");
			return null;
		} catch (IndexOutOfBoundException e) {
			log.error("**** VSDNMigrator (ErrorCode : -5) **** : Index Out of Bounds Exception in createPort");
			return null;
		}

	}

	// This Function connects the migrated host to the new switch on the newly
	// created port using the above functions
	public Host connectHost(long vDpid, short vPort, String mac) {
		try {

			MACAddress macAddr = MACAddress.valueOf(mac);

			// check for valid arguments
			HandlerUtils.isValidOVXPort(tenantId, vDpid, vPort);
			HandlerUtils.isUniqueHostMAC(macAddr);

			// connect the host to the new swtich
			Host host = virtualNetwork.connectHost(vDpid, vPort, macAddr);

			if (host == null) {
				log.error("**** VSDNMigrator (ErrorCode : -99) **** : Inernal Error in connectHost");
				return null;
			}

			log.info("**** VSDNMigrator **** : Successfully connected the virtual Host "
					+ mac + " on switch " + vDpid);
			return host;

		} catch (final InvalidTenantIdException e) {
			log.error("**** VSDNMigrator (ErrorCode : -1) **** : Invalid Tenant ID in connectHost");
			return null;
		} catch (final InvalidHostException e) {
			log.error("**** VSDNMigrator (ErrorCode : -2) **** : Invalid Host ID in connectHost");
			return null;
		} catch (final NetworkMappingException e) {
			log.error("**** VSDNMigrator (ErrorCode : -3) **** : Network Mapping Exception in connectHost");
			return null;
		} catch (final InvalidPortException e) {
			log.error("**** VSDNMigrator (ErrorCode : -6) **** : Invalid Port Exception in connectHost");
			return null;
		} catch (final InvalidDPIDException e) {
			log.error("**** VSDNMigrator (ErrorCode : -7) **** : Invalid dpid Exception in connectHost");
			return null;
		} catch (IndexOutOfBoundException e) {
			log.error("**** VSDNMigrator (ErrorCode : -5) **** : Index Out of Bounds Exception in connectHost");
			return null;
		}

	}

}
