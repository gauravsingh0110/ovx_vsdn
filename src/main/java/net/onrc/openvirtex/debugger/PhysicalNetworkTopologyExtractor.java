package net.onrc.openvirtex.debugger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;

public class PhysicalNetworkTopologyExtractor {
	private PhysicalNetwork physicalNetwork;
	OVXMap map;
	private final Logger log = LogManager
			.getLogger(PhysicalNetworkTopologyExtractor.class.getName());
	private Timer timer;

	public PhysicalNetworkTopologyExtractor() {
		// TODO Auto-generated constructor stub
		physicalNetwork = PhysicalNetwork.getInstance();
		DBHandler.connect();
		timer = new Timer();
		timer.schedule(new periodicRecorder(), 0, 5000);

	}

	private class periodicRecorder extends TimerTask {
		public void run() {
			recordData();
		}

		private void recordData() {
			// Getting physical hosts
			map = OVXMap.getInstance();
			Collection<OVXNetwork> vnets = map.listVirtualNetworks().values();
			long time = System.currentTimeMillis();
			Map<String, Object> pm = null;
			String query = null;

			for (OVXNetwork vnet : vnets) {
				for (Host h : vnet.getHosts()) {

					pm = h.convertToPhysical();
					if (pm != null && !pm.isEmpty()) {
						String sql = "SELECT * FROM Host_Entries WHERE MAC='"
								+ h.getMac() + "' AND pSwitch_ID='"
								+ pm.get("dpid") + "' AND pSwitch_Port='"
								+ pm.get("port") + "' AND vTenant_ID='"
								+ h.getPort().getTenantId()
								+ "' AND vHost_ID='" + h.getHostId()
								+ "' AND vSwitch_ID='"
								+ h.getPort().getParentSwitch().getSwitchName()
								+ "' AND vSwitch_Port='"
								+ h.getPort().getPortNumber()
								+ "' AND LastSeen >= " + (time - 6000) + ";";
						// log.info("Query : " + sql);
						ResultSet rs = DBHandler.getValues(sql);
						try {
							if (!rs.isBeforeFirst()) {
								query = "INSERT INTO Host_Entries VALUES('"
										+ pm.get("ipAddress")
										+ "','"
										+ h.getMac()
										+ "','"
										+ pm.get("dpid")
										+ "','"
										+ pm.get("port")
										+ "','"
										+ h.getPort().getTenantId()
										+ "','"
										+ h.getHostId()
										+ "','"
										+ h.getIp().toSimpleString()
										+ "','"
										+ h.getPort().getParentSwitch()
												.getSwitchName() + "','"
										+ h.getPort().getPortNumber() + "',"
										+ time + "," + time + ");";

							} else {
								rs.next();
								long firstSeen = rs.getLong("FirstSeen");
								query = "UPDATE Host_Entries SET LastSeen="
										+ time
										+ ", Physical_IP='"
										+ pm.get("ipAddress")
										+ "', Virtual_IP='"
										+ h.getIp().toSimpleString()
										+ "' WHERE MAC='"
										+ h.getMac()
										+ "' AND pSwitch_ID='"
										+ pm.get("dpid")
										+ "' AND pSwitch_Port='"
										+ pm.get("port")
										+ "' AND vTenant_ID='"
										+ h.getPort().getTenantId()
										+ "' AND vHost_ID='"
										+ h.getHostId()

										+ "' AND vSwitch_ID='"
										+ h.getPort().getParentSwitch()
												.getSwitchName()
										+ "' AND vSwitch_Port='"
										+ h.getPort().getPortNumber()
										+ "' AND FirstSeen=" + firstSeen + ";";

							}
						} catch (NumberFormatException | SQLException e) {
							log.error(e.toString());
						}
						// //log.info("Query : " + query);
						DBHandler.insertValues(query);

						// Insert in link table
						String nsql = "SELECT * FROM Link_Entries WHERE Src_Switch_ID='"
								+ pm.get("dpid").toString()
								+ "' AND Src_Switch_Port='"
								+ pm.get("port").toString()
								+ "' AND Dst_Switch_ID='"
								+ pm.get("dpid").toString()
								+ "' AND Dst_Switch_Port='"
								+ pm.get("port").toString()
								+ "' AND LastSeen >= " + (time - 6000) + ";";

						// log.info("Query : " + nsql);
						ResultSet nrs = DBHandler.getValues(nsql);
						try {
							if (!nrs.isBeforeFirst()) {

								query = "INSERT INTO Link_Entries VALUES("
										+ "'true','"
										+ pm.get("dpid").toString() + "','"
										+ pm.get("port").toString() + "','"
										+ pm.get("dpid").toString() + "','"
										+ pm.get("port").toString() + "',"
										+ time + "," + time + ");";

							} else {
								nrs.next();
								long firstSeen = nrs.getLong("FirstSeen");
								query = "UPDATE Link_Entries SET LastSeen="
										+ time + " WHERE Src_Switch_ID='"
										+ pm.get("dpid").toString()
										+ "' AND Src_Switch_Port='"
										+ pm.get("port").toString()
										+ "' AND Dst_Switch_ID='"
										+ pm.get("dpid").toString()
										+ "' AND Dst_Switch_Port='"
										+ pm.get("port").toString()
										+ "' AND FirstSeen=" + firstSeen + ";";

							}
						} catch (NumberFormatException | SQLException
								| NullPointerException e) {
							log.error(e.toString());
						}
						// log.info("Query : " + query);
						DBHandler.insertValues(query);
					}
					/*
					 * log.info("*****************  Hosts : " +
					 * pm.get("ipAddress").toString() + " " + h.getMac() + " " +
					 * pm.get("dpid").toString() + " " + pm.get("port") + " " +
					 * h.getPort().getTenantId() + " " + h.getHostId() + " " +
					 * h.getIp().toSimpleString() + " " +
					 * h.getPort().getParentSwitch().getSwitchName() + " " +
					 * h.getPort().getPortNumber() + " " + time + " " + time);
					 */
					// hosts.add(h);
				}
			}

			final Map<Integer, OVXNetwork> nets = OVXMap.getInstance()
					.listVirtualNetworks();
			// JSONRPC2Response wants a List, not a Set
			final List<Integer> list = new ArrayList<Integer>(nets.keySet());

			// Getting network topology
			if (physicalNetwork.getSwitches().size() != 0) {

				Set<PhysicalSwitch> el = new HashSet<PhysicalSwitch>();
				el.addAll(physicalNetwork.getSwitches());

				Iterator i = el.iterator();
				while (i.hasNext()) {
					PhysicalSwitch s = (PhysicalSwitch) i.next();
					// log.info("******************* Neighbours ",physicalNetwork.getNeighbors(s));
					// log.info("***************** Switchtes : " +
					// s.toString());
				}
			}

			if (physicalNetwork.getLinks().size() != 0) {

				Set<PhysicalLink> el = new HashSet<PhysicalLink>();
				el.addAll(physicalNetwork.getLinks());

				Iterator e = el.iterator();
				while (e.hasNext()) {
					// log.info("***************** Links : " +
					// e.next().toString());
					PhysicalLink pl = (PhysicalLink) e.next();
					// log.info("***************** Links : " + pl.toString());
					// Insert in link table
					String nsql = "SELECT * FROM Link_Entries WHERE Src_Switch_ID='"
							+ pl.getSrcSwitch().getSwitchName()
							+ "' AND Src_Switch_Port='"
							+ pl.getSrcPort().getPortNumber()
							+ "' AND Dst_Switch_ID='"
							+ pl.getDstSwitch().getSwitchName()
							+ "' AND Dst_Switch_Port='"
							+ pl.getDstPort().getPortNumber()
							+ "' AND LastSeen >= " + (time - 6000) + ";";

					// log.info("Query : " + nsql);
					ResultSet nrs = DBHandler.getValues(nsql);
					try {
						if (!nrs.isBeforeFirst()) {

							query = "INSERT INTO Link_Entries VALUES("
									+ "'false','"
									+ pl.getSrcSwitch().getSwitchName() + "','"
									+ pl.getSrcPort().getPortNumber() + "','"
									+ pl.getDstSwitch().getSwitchName() + "','"
									+ pl.getDstPort().getPortNumber() + "',"
									+ time + "," + time + ");";

						} else {

							query = "UPDATE Link_Entries SET LastSeen=" + time
									+ " WHERE Src_Switch_ID='"
									+ pl.getSrcSwitch().getSwitchName()
									+ "' AND Src_Switch_Port='"
									+ pl.getSrcPort().getPortNumber()
									+ "' AND Dst_Switch_ID='"
									+ pl.getDstSwitch().getSwitchName()
									+ "' AND Dst_Switch_Port='"
									+ pl.getDstPort().getPortNumber() + "';";

						}
					} catch (NumberFormatException | SQLException
							| NullPointerException er) {
						log.error(er.toString());
					}
					// log.info("Query : " + query);
					DBHandler.insertValues(query);
				}

			}

			if (physicalNetwork.getUplinkList() != null
					&& physicalNetwork.getUplinkList().size() != 0) {
				Iterator e = physicalNetwork.getUplinkList().iterator();
				while (e.hasNext()) {
					log.info("***************** UpLinks : "
							+ e.next().toString());
				}
			}
		}
	}

}
