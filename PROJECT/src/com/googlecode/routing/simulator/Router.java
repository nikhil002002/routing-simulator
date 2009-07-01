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
			if(r.ipAddress.equals(senderIpAddress) && r.port == senderPort) {
				cost = minimumPathTable.get(r.id).cost;
				gateway = r;
				System.out.println("["+this.routerInfo.id + "]: Recebi de "+r.id);
				
//				if(cost ==  UNAVAILABLE) {
//					cost = receivedMap.get(this.routerInfo.id).cost;
//					minimumPathTable.get(r.id).cost = cost;
//				}
				
				lastPing.put(r.id, System.currentTimeMillis());
				
			}
		}
		
		
		for(Long id: receivedMap.keySet()) {
			PathInfo receivedPathInfo = receivedMap.get(id);
			PathInfo actualPathInfo = minimumPathTable.get(id);
			if(actualPathInfo == null || receivedPathInfo.cost + cost < actualPathInfo.cost) {
				if(actualPathInfo == null) { 
					actualPathInfo = new PathInfo();
					actualPathInfo.destinationRouterID = receivedPathInfo.destinationRouterID;
				}
				System.out.println("["+this.routerInfo.id + "]: Houveram mudanças na minha tabela ");
				System.out.println("["+this.routerInfo.id + "]: O custo para "+actualPathInfo.destinationRouterID+ " era "+actualPathInfo.cost +
						" e agora eh "+ (receivedPathInfo.cost + cost));
				
				actualPathInfo.cost = receivedPathInfo.cost + cost;
				actualPathInfo.gatewayRouterID = gateway.id;
				
				minimumPathTable.put(id, actualPathInfo);
			}
		}
		
//		for (Entry<Long,Long> e : lastPing.entrySet()) {
//			if(e.getValue() > 5*SLEEP_TIME) {
//				e.setValue((long) UNAVAILABLE);
//				PathInfo actualPathInfo = minimumPathTable.get(e.getKey());
//				actualPathInfo.cost =  UNAVAILABLE;
//			}
//			
//		}
		
		
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
