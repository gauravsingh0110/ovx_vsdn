package net.onrc.openvirtex.migrator;

import java.util.HashMap;
import java.util.Map;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.onrc.openvirtex.api.service.handlers.ApiHandler;
import net.onrc.openvirtex.api.service.handlers.HandlerUtils;
import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.api.service.handlers.tenant.ConnectHost;
import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.exceptions.DuplicateMACException;
import net.onrc.openvirtex.exceptions.InvalidPortException;
import net.onrc.openvirtex.exceptions.InvalidTenantIdException;
import net.onrc.openvirtex.exceptions.MissingRequiredField;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class MigrateVM extends ApiHandler<Map<String, Object>> {

    Logger log = LogManager.getLogger(ConnectHost.class.getName());
	private VSDNMigrator vsdnm=new VSDNMigrator();
	Host host;
	boolean success;

    @Override
    public JSONRPC2Response process(final Map<String, Object> params) {
        JSONRPC2Response resp = null;
        success=false;
        try {
            final Number tenantId = HandlerUtils.<Number>fetchField(
                    TenantHandler.TENANT, params, true, null);
            final Number hid = HandlerUtils.<Number>fetchField(
                    TenantHandler.VSDN_HOST_ID, params, true, null);
            final String hmac = HandlerUtils.<String>fetchField(
                    TenantHandler.VSDN_HOST_MAC, params, true, null);
            final String svdpid = HandlerUtils.<String>fetchField(
                    TenantHandler.VSDN_SWITCH_VIRTUAL_DPID, params, true, null);
            final String spdpid = HandlerUtils.<String>fetchField(
                    TenantHandler.VSDN_SWITCH_PHYSICAL_DPID, params, true, null);
            final Number oport = HandlerUtils.<Number>fetchField(
                    TenantHandler.VSDN_OLD_PORT, params, true, null);
            final Number nport = HandlerUtils.<Number>fetchField(
                    TenantHandler.VSDN_NEW_PORT, params, true, null);
            
            log.info("********* Got the python ********"+tenantId + " "+hid+ " "+hmac+ " "+svdpid+ " "+spdpid+ " "+oport+ " "+nport);
            
            vsdnm.openVirtualNetwork(tenantId.intValue());
			vsdnm.disconnectHost(hid.intValue());
			OVXPort ovxPort=vsdnm.createPort(Long.parseLong(spdpid.replace(":", ""),16), nport.shortValue());
			if(ovxPort != null){
				host=vsdnm.connectHost(Long.parseLong(svdpid.replace(":", ""),16),ovxPort.getPortNumber(), hmac);
				
			}
			if(ovxPort != null && host != null){
				success=true;
				log.info("Successfylly migrated the host");
			}
			else{
				log.error("Some error occured, check Logs");
				}
			          
			Map<String, Object> reply = new HashMap<String, Object>();
			reply.put("success", success);
			resp = new JSONRPC2Response(reply, 0);

            /*HandlerUtils.isValidTenantId(tenantId.intValue());
           // HandlerUtils.isValidOVXPort(tenantId.intValue(), dpid.longValue(),
            //        port.shortValue());

            final OVXMap map = OVXMap.getInstance();
            final OVXNetwork virtualNetwork = map.getVirtualNetwork(tenantId
                    .intValue());
            final MACAddress macAddr = MACAddress.valueOf(mac);
            HandlerUtils.isUniqueHostMAC(macAddr);
            final Host host = virtualNetwork.connectHost(dpid.longValue(),
                    port.shortValue(), macAddr);

            if (host == null) {
                resp = new JSONRPC2Response(
                        new JSONRPC2Error(
                                JSONRPC2Error.INTERNAL_ERROR.getCode(),
                                this.cmdName()), 0);
            } else {
                this.log.info(
                        "Connected host with id {} and mac {} to virtual port {} on virtual switch {} in virtual network {}",
                        host.getHostId(), host.getMac().toString(), host
                                .getPort().getPortNumber(), host.getPort()
                                .getParentSwitch().getSwitchName(),
                        virtualNetwork.getTenantId());
                Map<String, Object> reply = new HashMap<String, Object>(
                        host.getDBObject());
                reply.put(TenantHandler.TENANT, tenantId.intValue());
                resp = new JSONRPC2Response(reply, 0);
            }
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
       /*( } catch (final IndexOutOfBoundException e) {
            resp = new JSONRPC2Response(
                    new JSONRPC2Error(
                            JSONRPC2Error.INVALID_PARAMS.getCode(),
                            this.cmdName()
                                    + ": Impossible to create the virtual port, too many ports on this virtual switch : "
                                    + e.getMessage()), 0);
        } catch (final NetworkMappingException e) {
            resp = new JSONRPC2Response(new JSONRPC2Error(
                    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
                            + ": " + e.getMessage()), 0);*/
        } catch (final DuplicateMACException e) {
            resp = new JSONRPC2Response(new JSONRPC2Error(
                    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
                            + ": " + e.getMessage()), 0);
        }

        return resp;
    }

    @Override
    public JSONRPC2ParamsType getType() {
        return JSONRPC2ParamsType.OBJECT;
    }


}