package com.googlecode.routing.simulator;

import java.io.PrintStream;
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

	public static final int UNAVAILABLE = -1;
	public static final long SLEEP_TIME = 4000;
	public final Map<Long, PathInfo> minimumPathTable;
	public final RouterInfo routerInfo;
	public final Set<LinkInfo> links;
	public final Set<RouterInfo> adjacentRouters;
	public Map<Long, Long> lastPing = new HashMap<Long, Long>();
	private final double networkDiameter;
	
	public final PrintStream out;

	public DatagramSocket serverSocket;

	public Router(RouterInfo routerInfo, Set<RouterInfo> adjacentRouters, Set<LinkInfo> links, PrintStream out, double maxCountToInfinity) {

		this.routerInfo = routerInfo;
		this.adjacentRouters = adjacentRouters;
		this.links = links;
		this.minimumPathTable = new HashMap<Long, PathInfo>();
		this.networkDiameter = maxCountToInfinity;
		this.out = out;

		for (LinkInfo info : links) {
			PathInfo path = new PathInfo();
			path.cost = info.cost;
			path.destinationRouterID = (info.routerAID == routerInfo.id) ? info.routerBID : info.routerAID;
			path.gatewayRouterID = path.destinationRouterID;
			minimumPathTable.put(path.destinationRouterID, path);
		}
		
		this.printDistanceTable();
	}

	public void initSocket() throws SocketException, UnknownHostException {
		serverSocket = new DatagramSocket(routerInfo.port, routerInfo.ipAddress);
		System.out.println("Ouvindo em: " + routerInfo.ipAddress + ":" + routerInfo.port);
	}

	public RouterInfo getAdjacentByIPAndPort(InetAddress inetAddr, int port) {
		for (RouterInfo info : adjacentRouters) {
			if (info.ipAddress.equals(inetAddr) && info.port == port) {
				return info;
			}
		}
		return null;
	}
	
	public void printDistanceTable() {
		out.println("| ID |  GATEWAY |  COST  |");
		for (Entry<Long, PathInfo> entry : minimumPathTable.entrySet()) {
			out.println("| " + entry.getKey() + " |  " + entry.getValue().gatewayRouterID + " |  " + entry.getValue().cost + "  |");
		}
		out.println();
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
