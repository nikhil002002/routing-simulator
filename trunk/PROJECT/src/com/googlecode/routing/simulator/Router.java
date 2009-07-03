package com.googlecode.routing.simulator;

import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * @author Renato Miceli
 */
public class Router {

	public static final long SLEEP_TIME = 4000;
	public static final double INFINITY = Double.MAX_VALUE;

	public final Map<Long, Map<Long, PathInfo>> minimumPathTable;
	public final RouterInfo routerInfo;
	public final Map<Long, LinkInfo> links;
	public final Set<RouterInfo> adjacentRouters;
	public final double maxCountToInfinity;
	public final PrintStream out;

	public final Map<Long, Long> lastPing;
	public DatagramSocket serverSocket;

	public Router(RouterInfo routerInfo, Set<RouterInfo> adjacentRouters, Map<Long, LinkInfo> links, PrintStream out, double maxCountToInfinity) {

		this.routerInfo = routerInfo;
		this.adjacentRouters = adjacentRouters;
		this.links = links;
		this.minimumPathTable = new HashMap<Long, Map<Long, PathInfo>>();
		this.lastPing = new HashMap<Long, Long>();
		this.maxCountToInfinity = maxCountToInfinity;
		this.out = out;

		Map<Long, PathInfo> distanceVector = new HashMap<Long, PathInfo>();
		// for (LinkInfo info : links.values()) {
		// PathInfo path = new PathInfo();
		// path.cost = info.cost;
		// path.destinationRouterID = (info.routerAID == routerInfo.id) ?
		// info.routerBID : info.routerAID;
		// path.gatewayRouterID = path.destinationRouterID;
		// distanceVector.put(path.destinationRouterID, path);
		// }
		PathInfo pathToMyself = new PathInfo();
		pathToMyself.cost = 0;
		pathToMyself.destinationRouterID = routerInfo.id;
		pathToMyself.gatewayRouterID = pathToMyself.destinationRouterID;
		distanceVector.put(pathToMyself.destinationRouterID, pathToMyself);
		this.minimumPathTable.put(this.routerInfo.id, distanceVector);

		this.printDistanceTable();
	}

	public Map<Long, PathInfo> getDistanceTable() {
		return this.minimumPathTable.get(this.routerInfo.id);
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

	public boolean relaxEdges(long changedVectorRouterID) {

		Map<Long, PathInfo> receivedMap = minimumPathTable.get(changedVectorRouterID);
		Map<Long, PathInfo> myDistanceTable = getDistanceTable();

		PathInfo pathFromMeToDistanceVectorOwner = myDistanceTable.get(changedVectorRouterID);
		if (pathFromMeToDistanceVectorOwner == null) {
			pathFromMeToDistanceVectorOwner = new PathInfo();
			pathFromMeToDistanceVectorOwner.cost = links.get(changedVectorRouterID).cost;
			pathFromMeToDistanceVectorOwner.gatewayRouterID = changedVectorRouterID;
			pathFromMeToDistanceVectorOwner.destinationRouterID = changedVectorRouterID;
			myDistanceTable.put(changedVectorRouterID, pathFromMeToDistanceVectorOwner);
		} else if (pathFromMeToDistanceVectorOwner.cost == Router.INFINITY) {
			pathFromMeToDistanceVectorOwner.cost = links.get(changedVectorRouterID).cost;
		}

		Map<Long, PathInfo> minDistances = new HashMap<Long, PathInfo>();
		for (PathInfo info : myDistanceTable.values()) {
			PathInfo newInfo = new PathInfo();
			newInfo.cost = links.containsKey(info.destinationRouterID) ? links.get(info.destinationRouterID).cost : Router.INFINITY;
			newInfo.gatewayRouterID = -1;
			newInfo.destinationRouterID = info.destinationRouterID;
			minDistances.put(newInfo.destinationRouterID, newInfo);
		}
		PathInfo myInfo = new PathInfo();
		myInfo.cost = 0;
		myInfo.gatewayRouterID = changedVectorRouterID;
		myInfo.destinationRouterID = changedVectorRouterID;
		minDistances.put(myInfo.destinationRouterID, myInfo);
		

		System.out.println(myDistanceTable);
		System.out.println(minDistances);

		for (PathInfo receivedPath : receivedMap.values()) {
			if (receivedPath.destinationRouterID == changedVectorRouterID) {
				continue;
			}
			if (!minDistances.containsKey(receivedPath.destinationRouterID)) {
				PathInfo newInfo = new PathInfo();
				newInfo.destinationRouterID = receivedPath.destinationRouterID;
				newInfo.gatewayRouterID = changedVectorRouterID;
				newInfo.cost = receivedPath.cost + pathFromMeToDistanceVectorOwner.cost;
				minDistances.put(newInfo.destinationRouterID, newInfo);
			} else {
				PathInfo pathFromMeToDestination = minDistances.get(receivedPath.destinationRouterID);
				if (pathFromMeToDestination.cost > receivedPath.cost + pathFromMeToDistanceVectorOwner.cost) {
					pathFromMeToDestination.cost = receivedPath.cost + pathFromMeToDistanceVectorOwner.cost;
					pathFromMeToDestination.gatewayRouterID = changedVectorRouterID;
				}
			}
		}
		
		for (Entry<Long, PathInfo> entry : minDistances.entrySet()) {
			if(links.containsKey(entry.getKey())) {
				LinkInfo link = links.get(entry.getKey());
				PathInfo path = entry.getValue();
				if(link.cost < path.cost) {
					path.cost = link.cost;
					path.gatewayRouterID = (link.routerAID == changedVectorRouterID) ? link.routerBID : link.routerAID;
				}
			}
		}

		System.out.println(minDistances);

		boolean changed = false;
		for (Entry<Long, PathInfo> entry : minDistances.entrySet()) {
			System.out.println(entry.getValue());
			if (!myDistanceTable.containsKey(entry.getKey())) {
				myDistanceTable.put(entry.getKey(), entry.getValue());
				changed = true;
			} else {
				PathInfo oldPath = myDistanceTable.get(entry.getKey());
				if (oldPath.cost != entry.getValue().cost || oldPath.gatewayRouterID != entry.getValue().gatewayRouterID) {
					oldPath.cost = entry.getValue().cost;
					oldPath.gatewayRouterID = entry.getValue().gatewayRouterID;
					changed = true;
				}
			}
		}

		return changed;
	}

	public void printDistanceTable() {
		out.println("| ID |  COST  |  GATEWAY |");
		for (PathInfo info : minimumPathTable.get(routerInfo.id).values()) {
			out.println("|  " + info.destinationRouterID + " |   " + (info.cost == INFINITY ? "N/A" : info.cost) + "  |     " + info.gatewayRouterID
					+ "    |");
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
		Map<Long, PathInfo> map = new HashMap<Long, PathInfo>();
		for (String s : string.split("\n")) {
			PathInfo info = PathInfo.buildPathInfo(s);
			map.put(info.destinationRouterID, info);
		}
		return map;
	}

}
