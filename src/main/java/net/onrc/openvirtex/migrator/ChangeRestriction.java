package net.onrc.openvirtex.migrator;

import java.util.HashMap;
import java.util.Map;

import net.onrc.openvirtex.api.service.handlers.ApiHandler;
import net.onrc.openvirtex.api.service.handlers.HandlerUtils;
import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.api.service.handlers.tenant.ConnectHost;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.exceptions.DuplicateMACException;
import net.onrc.openvirtex.exceptions.InvalidPortException;
import net.onrc.openvirtex.exceptions.InvalidTenantIdException;
import net.onrc.openvirtex.exceptions.MissingRequiredField;
import net.onrc.openvirtex.exceptions.NetworkMappingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class ChangeRestriction extends ApiHandler<Map<String, Object>> {

    Logger log = LogManager.getLogger(ChangeRestriction.class.getName());
	String message;;

    @Override
    public JSONRPC2Response process(final Map<String, Object> params) {
        JSONRPC2Response resp = null;
        message="failed";
        try {
            final Number tenantId = HandlerUtils.<Number>fetchField(
                    TenantHandler.TENANT, params, true, null);
            final String isRestricted = HandlerUtils.<String>fetchField(
                    TenantHandler.VSDN_TYPE_OF_TOPOLOGY, params, true, null);
            
	    	OVXMap map = OVXMap.getInstance();
	    	OVXNetwork ovxNetwork=map.getVirtualNetwork(tenantId.intValue());
	    	//current topology restriction
	    	final boolean currentTopologyRestriction=ovxNetwork.getTopologyRestriction();
	    	//Topology restrictions can be changed in runtime
	    	if(isRestricted.equalsIgnoreCase("true")){
	    		ovxNetwork.setTopologyRestriction(true);
	    		message="success";
	    	}
	    	if(isRestricted.equalsIgnoreCase("false")){
	    		ovxNetwork.setTopologyRestriction(false);
	    		message="success";
	    	}

            log.info("Changing the restriction of virtula network "+tenantId + " to "+isRestricted);

            Map<String, Object> reply = new HashMap<String, Object>();
			reply.put("message", message);
			reply.put("previous_value", currentTopologyRestriction);
			reply.put("current_value", ovxNetwork.getTopologyRestriction());
			resp = new JSONRPC2Response(reply, 0);


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
        } catch (final DuplicateMACException e) {
            resp = new JSONRPC2Response(new JSONRPC2Error(
                    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
                            + ": " + e.getMessage()), 0);
        } catch (final NetworkMappingException e) {
            resp = new JSONRPC2Response(new JSONRPC2Error(
                    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
                            + ": Invalid tenant id : " + e.getMessage()), 0);
		}

        return resp;
    }

    @Override
    public JSONRPC2ParamsType getType() {
        return JSONRPC2ParamsType.OBJECT;
    }


}