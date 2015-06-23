package net.onrc.openvirtex.debugger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.onrc.openvirtex.api.service.handlers.ApiHandler;
import net.onrc.openvirtex.api.service.handlers.HandlerUtils;
import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.exceptions.DuplicateMACException;
import net.onrc.openvirtex.exceptions.InvalidPortException;
import net.onrc.openvirtex.exceptions.InvalidTenantIdException;
import net.onrc.openvirtex.exceptions.MissingRequiredField;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class TraceRoute extends ApiHandler<Map<String, Object>> {

	Logger log = LogManager.getLogger(TraceRoute.class.getName());
	String message;

	@Override
	public JSONRPC2Response process(final Map<String, Object> params) {
		JSONRPC2Response resp = null;
		try {
			final Number tenantId = HandlerUtils.<Number> fetchField(
					TenantHandler.TENANT, params, true, null);
			final String srcIP = HandlerUtils.<String> fetchField(
					TenantHandler.VSDN_SRC_IP, params, true, null);
			final String dstIP = HandlerUtils.<String> fetchField(
					TenantHandler.VSDN_DST_IP, params, true, null);
			final Number timeStamp = HandlerUtils.<Number> fetchField(
					TenantHandler.VSDN_TIMESTAMP, params, true, null);
			final String Protocol = HandlerUtils.<String> fetchField(
					TenantHandler.VSDN_PROTOCOL, params, true, null);

			List<String> path = traceRoute(srcIP, dstIP, tenantId.intValue(),
					timeStamp.longValue(), Protocol);

			Map<String, String> reply = new HashMap<String, String>();
			reply.put("message", message);
			reply.put("path", path.toString());
			log.info(path.toString());
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
		}

		return resp;
	}

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.OBJECT;
	}

	private List<String> traceRoute(String srcVIP, String dstVIP, int tenantID,
			long timeStamp, String protocolType) {
		String srcMAC, srcPIP, srcDpid, srcPort;
		String dstMAC, dstPIP, dstDpid, dstPort;
		ResultSet rs;
		String query;

		int safetyCounter = 0;

		List<String> path = new ArrayList<String>();
		try {

			query = "SELECT * FROM Host_Entries where Virtual_IP='" + srcVIP
					+ "' AND vTenant_ID='" + tenantID + "' AND LastSeen >="
					+ timeStamp + " AND FirstSeen <=" + timeStamp + ";";
			// log.info(query);
			rs = DBHandler.getValues(query);
			if (!rs.isBeforeFirst()) {
				message = "Invalid SRC IP";
				return path;
			}
			rs.next();
			srcPIP = rs.getString("Physical_IP");
			srcMAC = rs.getString("MAC");
			srcDpid = rs.getString("pSwitch_ID");
			srcPort = rs.getString("pSwitch_Port");

			query = "SELECT * FROM Host_Entries where Virtual_IP='" + dstVIP
					+ "' AND vTenant_ID='" + tenantID + "' AND LastSeen >="
					+ timeStamp + " AND FirstSeen <=" + timeStamp + ";";
			// log.info(query);
			rs = DBHandler.getValues(query);
			if (!rs.isBeforeFirst()) {
				message = "Invalid DST IP";
				return path;
			}
			rs.next();
			dstPIP = rs.getString("Physical_IP");
			dstMAC = rs.getString("MAC");
			dstDpid = rs.getString("pSwitch_ID");
			dstPort = rs.getString("pSwitch_Port");
			// Assuming Hard Timeout to be 60 seconds

			path.add("ip:" + srcVIP);
			path.add("port:" + srcPort);
			path.add("dpid:" + srcDpid);

			query = "CREATE TABLE TempFlowMods AS (SELECT * FROM FlowMod_Entries where TimeStamp >= "
					+ (timeStamp - 60000)
					+ " AND TimeStamp <="
					+ timeStamp
					+ ");";
			// log.info(query);
			DBHandler.insertValues(query);

			query = "CREATE TABLE TempFlowRems AS (SELECT * FROM FlowRem_Entries where TimeStamp >= "
					+ (timeStamp - 60000)
					+ " AND TimeStamp <="
					+ timeStamp
					+ ");";
			// log.info(query);
			DBHandler.insertValues(query);

			query = "CREATE TABLE FinalFlowMods AS (SELECT * FROM TempFlowMods where (Match_dl_vlan,Match_in_port,Match_dl_src,Match_dl_dst,pSwitch_ID) NOT IN (Select Match_dl_vlan,Match_in_port,Match_dl_src,Match_dl_dst,pSwitch_ID from TempFlowRems));";
			DBHandler.insertValues(query);
			// log.info(query);

			query = "CREATE TABLE FinalLinkEntries AS (SELECT * FROM Link_Entries where LastSeen >="
					+ (timeStamp - 5000)
					+ "  AND FirstSeen <= "
					+ (timeStamp - 5000) + ");";
			// log.info(query);
			DBHandler.insertValues(query);

			// Crawler algorithm

			query = "SELECT Actions FROM FinalFlowMods where Match_nw_src='"
					+ srcVIP + "' AND Match_nw_dst='" + dstVIP
					+ "' AND Match_dl_dst='" + dstMAC + "' AND Match_dl_src='"
					+ srcMAC + "' AND Match_nw_protocol='" + protocolType
					+ "' AND pSwitch_ID='" + srcDpid + "' AND Match_in_port='"
					+ srcPort + "';";
			// log.info(query);
			rs = DBHandler.getValues(query);
			if (!rs.isBeforeFirst()) {
				message = "Could Not Find Complete Path";
				query = "DROP TABLE FinalLinkEntries;";
				DBHandler.insertValues(query);
				query = "DROP TABLE TempFlowMods;";
				DBHandler.insertValues(query);
				query = "DROP TABLE TempFlowRems;";
				DBHandler.insertValues(query);
				query = "DROP TABLE FinalFlowMods;";
				DBHandler.insertValues(query);
				return path;
			}
			String new_nw_src = null, new_nw_dst = null, new_dl_src = null, new_dl_dst = null, out_port = null;
			String srcSwitch = srcDpid;
			String srcSwitchPort = srcPort;
			boolean isEdge = false;
			boolean noPath = false;
			while (!isEdge && !noPath) {
				rs.next();
				String action = rs.getString("Actions");
				String[] sAction = action.split(",");
				if (sAction.length == 5) {
					new_nw_dst = sAction[0].split("\\[")[2].split("\\]")[0];
					new_nw_src = sAction[1].split("\\[")[1].split("\\]")[0];
					new_dl_src = sAction[2].split("\\[")[1].split("\\]")[0];
					new_dl_dst = sAction[3].split("\\[")[1].split("\\]")[0];
					out_port = sAction[4].split("\\[")[1].split("\\]")[0];
				}
				if (sAction.length == 3) {
					new_dl_src = sAction[0].split("\\[")[2].split("\\]")[0];
					new_dl_dst = sAction[1].split("\\[")[1].split("\\]")[0];
					out_port = sAction[2].split("\\[")[1].split("\\]")[0];

				}
				query = "SELECT * FROM FinalLinkEntries WHERE  Src_Switch_ID='"
						+ srcSwitch + "' AND Src_Switch_Port='" + out_port
						+ "';";
				path.add("port:" + out_port);
				// log.info(query);
				rs.close();
				rs = DBHandler.getValues(query);
				if (!rs.isBeforeFirst()) {
					message = "Could Not Find Complete Path";
					query = "DROP TABLE FinalLinkEntries;";
					DBHandler.insertValues(query);
					query = "DROP TABLE TempFlowMods;";
					DBHandler.insertValues(query);
					query = "DROP TABLE TempFlowRems;";
					DBHandler.insertValues(query);
					query = "DROP TABLE FinalFlowMods;";
					DBHandler.insertValues(query);
					return path;
				}
				rs.next();
				srcSwitch = rs.getString("Dst_Switch_ID");
				srcSwitchPort = rs.getString("Dst_Switch_Port");
				if (rs.getString("Edge_Link").equals("true")) {
					isEdge = true;
					// log.info("is Edge is True");
					query = "SELECT * FROM Host_Entries where pSwitch_ID='"
							+ srcSwitch + "' AND pSwitch_Port='"
							+ srcSwitchPort + "' AND LastSeen >=" + timeStamp
							+ " AND FirstSeen <=" + timeStamp + ";";

					// log.info(query);
					rs.close();
					rs = DBHandler.getValues(query);
					if (!rs.isBeforeFirst()) {
						message = "Could Not Find Complete Path";
						query = "DROP TABLE FinalLinkEntries;";
						DBHandler.insertValues(query);
						query = "DROP TABLE TempFlowMods;";
						DBHandler.insertValues(query);
						query = "DROP TABLE TempFlowRems;";
						DBHandler.insertValues(query);
						query = "DROP TABLE FinalFlowMods;";
						DBHandler.insertValues(query);
						return path;
					}
					rs.next();
					path.add("ip:" + rs.getString("Virtual_IP"));
					message = "Success";
					// log.info(path.toArray().toString());
				} else {
					path.add("port:" + srcSwitchPort);
					path.add("dpid:" + srcSwitch);
					query = "SELECT Actions FROM FinalFlowMods where Match_nw_src='"
							+ new_nw_src
							+ "' AND Match_nw_dst='"
							+ new_nw_dst
							+ "' AND Match_dl_dst='"
							+ new_dl_dst.toLowerCase()
							+ "' AND Match_dl_src='"
							+ new_dl_src.toLowerCase()
							+ "' AND Match_nw_protocol='"
							+ protocolType
							+ "' AND pSwitch_ID='"
							+ srcSwitch
							+ "' AND Match_in_port='" + srcSwitchPort + "';";
					// log.info(query);
					rs.close();
					rs = DBHandler.getValues(query);
					if (!rs.isBeforeFirst()) {
						message = "Could Not Find Complete Path";
						query = "DROP TABLE FinalLinkEntries;";
						DBHandler.insertValues(query);
						query = "DROP TABLE TempFlowMods;";
						DBHandler.insertValues(query);
						query = "DROP TABLE TempFlowRems;";
						DBHandler.insertValues(query);
						query = "DROP TABLE FinalFlowMods;";
						DBHandler.insertValues(query);
						return path;
					}
				}
				safetyCounter++;
				if (safetyCounter == 100) {
					noPath = true;
					message = "Could not find path within Safe limits of Recursion";
				}

			}

		} catch (SQLException e) {
			log.error(e.toString());
		}
		query = "DROP TABLE FinalLinkEntries;";
		DBHandler.insertValues(query);
		query = "DROP TABLE TempFlowMods;";
		DBHandler.insertValues(query);
		query = "DROP TABLE TempFlowRems;";
		DBHandler.insertValues(query);
		query = "DROP TABLE FinalFlowMods;";
		DBHandler.insertValues(query);
		return path;
	}

}