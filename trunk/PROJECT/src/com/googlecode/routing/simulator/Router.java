package com.googlecode.routing.simulator;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * @author Renato Miceli
 */
public class Router {

	private static final int  UNAVAILABLE = -1;
	private static final long SLEEP_TIME = 4000;
	private final Map<Long, PathInfo> minimumPathTable;
	private final RouterInfo routerInfo;
	private final Set<LinkInfo> links;
	private final Set<RouterInfo> adjacentRouters;
	private Map<Long, Long> lastPing = new HashMap<Long,Long>();

	private DatagramSocket serverSocket;

	public Router(RouterInfo routerInfo, Set<RouterInfo> adjacentRouters, Set<LinkInfo> links) {

		this.routerInfo = routerInfo;
		this.adjacentRouters = adjacentRouters;
		this.links = links;
		this.minimumPathTable = new HashMap<Long, PathInfo>();
		
		for (LinkInfo info : links) {
			PathInfo path = new PathInfo();
			path.cost = info.cost;
			path.destinationRouterID = (info.routerAID == routerInfo.id) ? info.routerBID : info.routerAID;
			path.gatewayRouterID = path.destinationRouterID;
			minimumPathTable.put(path.destinationRouterID, path);
		}
	}

	public void initSocket() throws SocketException, UnknownHostException {
		serverSocket = new DatagramSocket(routerInfo.port, routerInfo.ipAddress);
		System.out.println("Ouvindo em: " + routerInfo.ipAddress+":"+routerInfo.port);
	}

	public void startRouting() throws IOException, InterruptedException {
		while(true) {
			byte[] byteMap = serialize(minimumPathTable);
			for (RouterInfo router : adjacentRouters) {
				DatagramPacket sendPacket = new DatagramPacket(byteMap, byteMap.length, router.ipAddress, router.port);
				System.out.println("Enviando para "+router.ipAddress+ ":" + router.port);
				serverSocket.send(sendPacket);
			}
			Thread.sleep(SLEEP_TIME);
		}
	}
	
	public void listen() throws IOException {
		
		byte[] receiveData = new byte[1024]; 
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length); 
		serverSocket.receive(receivePacket);
		
		InetAddress senderIpAddress = receivePacket.getAddress();
		int senderPort = receivePacket.getPort();
		
		double cost=0;
		RouterInfo gateway = null;
		
		Map<Long, PathInfo> receivedMap = Router.deserialize(receivePacket.getData());
		
		for(RouterInfo r: adjacentRouters) {
			
			if(r.ipAddress.equals(senderIpAddress) && r.port == senderPort) { //identify the sender
				PathInfo actualPathInfo = minimumPathTable.get(r.id); //Info about the link to the sender
				
				if(actualPathInfo == null) { //If there's no link
					
					actualPathInfo = new PathInfo();
					actualPathInfo.destinationRouterID = r.id;
					actualPathInfo.gatewayRouterID = r.id;
					actualPathInfo.cost = receivedMap.get(routerInfo.id).cost;
					
					minimumPathTable.put(r.id, actualPathInfo); //creates a link
				}
				cost = actualPathInfo.cost; //cost of this link
				gateway = r; // stores the sender for future usage
				
				System.out.println("["+this.routerInfo.id + "]: Recebi de "+r.id);
				
				lastPing.put(r.id, System.currentTimeMillis()); //Mark PING from the sender
				
			}
		}
		
		for(Long id: receivedMap.keySet()) {
			
			PathInfo receivedPathInfo = receivedMap.get(id);
			PathInfo actualPathInfo = minimumPathTable.get(id);
			
			if(receivedPathInfo.destinationRouterID == routerInfo.id) { //If the target router is this one
				if(receivedPathInfo.cost == UNAVAILABLE) continue; //If the other router thinks I'm off, disconsider
				receivedPathInfo.destinationRouterID = gateway.id; //reverse to have the sender as the destination router
				receivedPathInfo.gatewayRouterID = gateway.id; //reverse to have the sender as the gateway
				
			}
			
			if(actualPathInfo == null || 
					receivedPathInfo.cost == UNAVAILABLE ^ actualPathInfo.cost == UNAVAILABLE ||
					 receivedPathInfo.cost + cost < actualPathInfo.cost	) { //If i don't have a link to this node, or the availability is different from what i have or the cost is lower
			
				if(actualPathInfo == null) { //If i don't have a link
					actualPathInfo = new PathInfo();
					actualPathInfo.destinationRouterID = receivedPathInfo.destinationRouterID;
					actualPathInfo.cost = receivedPathInfo.cost;
				}
				double previousCost = actualPathInfo.cost;
				
				if(receivedPathInfo.cost == UNAVAILABLE && actualPathInfo.cost != UNAVAILABLE) {
					actualPathInfo.cost = UNAVAILABLE;
					System.out.println("Fui avisado pelo roteador ["+gateway.id+"] que o roteador ["+actualPathInfo.destinationRouterID + "] estah indisponivel" );
				} else if (actualPathInfo.cost != UNAVAILABLE) {
					actualPathInfo.cost = receivedPathInfo.cost + cost;
				}
				
				if(previousCost != actualPathInfo.cost) {	
					System.out.println("["+this.routerInfo.id + "]: Houveram mudanças na minha tabela ");
					System.out.println("["+this.routerInfo.id + "]: O custo para "+actualPathInfo.destinationRouterID+ " era "+ previousCost +
							" e agora eh "+ (actualPathInfo.cost));
					
				}
				actualPathInfo.gatewayRouterID = gateway.id;
				
				minimumPathTable.put(id, actualPathInfo);
				printTable();
			}
		}
		
		
		
		
		
	}

	private void printTable() {
		System.out.println("| ID |  GATEWAY |  COST  |");
		for (Entry<Long,PathInfo> entry : minimumPathTable.entrySet()) {
			System.out.println("| "+entry.getKey()+" |  "+entry.getValue().gatewayRouterID+" |  "+entry.getValue().cost+"  |");
			
		}
		System.out.println();
	}

	public void verifyNeighbors() {
		for (Entry<Long,Long> e : lastPing.entrySet()) {
			if(System.currentTimeMillis() - e.getValue() > 5*SLEEP_TIME && minimumPathTable.get(e.getKey()).cost != UNAVAILABLE) {
				minimumPathTable.get(e.getKey()).cost = UNAVAILABLE;
				System.out.println("["+this.routerInfo.id+"] Timeout para resposta do roteador ["+e.getKey() + "] atingido, marcando como indisponivel" );
			}
			
		}
	}

	public static byte[] serialize(Map<Long, PathInfo> map) {
		StringBuilder bld = new StringBuilder();
		for (PathInfo entry : map.values()) {
			bld.append(entry.toString() + "\n");
		}
		String result = bld.toString();
		return result.substring(0, result.length() - 1).getBytes();
	}

	public static Map<Long, PathInfo> deserialize(byte[] array) {
		String string = new String(array);
		Map<Long, PathInfo> map = new LinkedHashMap<Long, PathInfo>();
		for (String s : string.split("\n")) {
			PathInfo info = PathInfo.buildPathInfo(s);
			map.put(info.destinationRouterID, info);
		}
		return map;
	}

}
