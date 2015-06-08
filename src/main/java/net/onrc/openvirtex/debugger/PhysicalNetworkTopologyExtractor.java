package net.onrc.openvirtex.debugger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.exceptions.AddressMappingException;

public class PhysicalNetworkTopologyExtractor {
	private PhysicalNetwork physicalNetwork;
	OVXMap map;
    private final Logger log = LogManager.getLogger(PhysicalNetworkTopologyExtractor.class.getName());
	private Timer timer ;	

	public PhysicalNetworkTopologyExtractor() {
		// TODO Auto-generated constructor stub
		physicalNetwork=PhysicalNetwork.getInstance();
		timer= new Timer();
		timer.schedule(new periodicRecorder(), 0, 5000);
		
		
		
	}
	
	private class periodicRecorder extends TimerTask {
	    public void run() {
	    	//Getting physical hosts 
	    	map = OVXMap.getInstance();
	    	Collection<OVXNetwork> vnets = map.listVirtualNetworks().values();

            for (OVXNetwork vnet : vnets) {
                for (Host h : vnet.getHosts()) {
                    log.info("*****************  Hosts : "+h.convertToPhysical().toString()+ " "+h.getIp());
                	//hosts.add(h);
                }
            }

            final Map<Integer, OVXNetwork> nets = OVXMap.getInstance()
                    .listVirtualNetworks();
            // JSONRPC2Response wants a List, not a Set
            final List<Integer> list = new ArrayList<Integer>(nets.keySet());

            
           
         
	    	// Getting network topology
	    	if(physicalNetwork.getSwitches().size()!=0){
	    		
	    		Iterator e =physicalNetwork.getSwitches().iterator();
	    		while (e.hasNext()){
	    			PhysicalSwitch s=(PhysicalSwitch)e.next();
	    			//log.info("******************* Neighbours ",physicalNetwork.getNeighbors(s));
	    			log.info("***************** Switchtes : " + s.toString());
	    		}
	    	}
	    	
	    	if(physicalNetwork.getLinks().size()!=0){
	    		
	    		Iterator e =physicalNetwork.getLinks().iterator();
	    		while (e.hasNext()){
	    			log.info("***************** Links : " + e.next().toString());
	    		}
	    	}
	    	
	    	
	    	
	    	if(physicalNetwork.getUplinkList()!= null && physicalNetwork.getUplinkList().size()!=0){
	    		Iterator e =physicalNetwork.getUplinkList().iterator();
	    		while (e.hasNext()){
	    			log.info("***************** UpLinks : " + e.next().toString());
	    		}
	    	}
	    
	    }
	 }

	
}
