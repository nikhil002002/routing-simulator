package com.googlecode.routing.simulator;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Renato Miceli
 */
public class Router {

	public static final int UNAVAILABLE = -1;
	public static final long SLEEP_TIME = 4000;
	public final Map<Long, PathInfo> minimumPathTable;
	public final RouterInfo routerInfo;
	private final Set<LinkInfo> links;
	public final Set<RouterInfo> adjacentRouters;
	public Map<Long, Long> lastPing = new HashMap<Long, Long>();
	private final double networkDiameter;

	public DatagramSocket serverSocket;

	public Router(RouterInfo routerInfo, Set<RouterInfo> adjacentRouters, Set<LinkInfo> links, double maxCountToInfinity) {

		this.routerInfo = routerInfo;
		this.adjacentRouters = adjacentRouters;
		this.links = links;
		this.minimumPathTable = new HashMap<Long, PathInfo>();
		this.networkDiameter = maxCountToInfinity;

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
		System.out.println("Ouvindo em: " + routerInfo.ipAddress + ":" + routerInfo.port);
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
