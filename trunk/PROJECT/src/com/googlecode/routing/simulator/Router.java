package com.googlecode.routing.simulator;

import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
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
		for (LinkInfo info : links.values()) {
			PathInfo path = new PathInfo();
			path.cost = info.cost;
			path.destinationRouterID = (info.routerAID == routerInfo.id) ? info.routerBID : info.routerAID;
			path.gatewayRouterID = path.destinationRouterID;
			distanceVector.put(path.destinationRouterID, path);
		}
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

		Map<Long, PathInfo> myDistanceTable = getDistanceTable();
		Map<Long, PathInfo> beforeList = new HashMap<Long, PathInfo>();
		for (Entry<Long, PathInfo> entry : myDistanceTable.entrySet()) {
			beforeList.put(entry.getKey(), new PathInfo(entry.getValue()));
		}

		for (PathInfo path : myDistanceTable.values()) {
			if (path.gatewayRouterID == changedVectorRouterID && path.destinationRouterID != routerInfo.id) {
				path.cost = Router.INFINITY;
			}
		}

		Map<Long, PathInfo> receivedMap = minimumPathTable.get(changedVectorRouterID);

		PathInfo pathFromMeToDistanceVectorOwner = myDistanceTable.get(changedVectorRouterID);
		if (lastPing.containsKey(changedVectorRouterID) && lastPing.get(changedVectorRouterID) > 0 && links.containsKey(changedVectorRouterID)
				&& pathFromMeToDistanceVectorOwner.cost > links.get(changedVectorRouterID).cost) {
			pathFromMeToDistanceVectorOwner.cost = links.get(changedVectorRouterID).cost;
			pathFromMeToDistanceVectorOwner.gatewayRouterID = changedVectorRouterID;
		}

		for (PathInfo receivedPath : receivedMap.values()) {
			if (!myDistanceTable.containsKey(receivedPath.destinationRouterID)) {
				PathInfo newInfo = new PathInfo();
				newInfo.destinationRouterID = receivedPath.destinationRouterID;
				newInfo.gatewayRouterID = changedVectorRouterID;
				newInfo.cost = receivedPath.cost + pathFromMeToDistanceVectorOwner.cost;
				myDistanceTable.put(newInfo.destinationRouterID, newInfo);
				if (newInfo.cost > maxCountToInfinity) {
					newInfo.cost = maxCountToInfinity;
				}
			} else {
				PathInfo pathFromMeToDestination = myDistanceTable.get(receivedPath.destinationRouterID);
				if (pathFromMeToDestination.cost > receivedPath.cost + pathFromMeToDistanceVectorOwner.cost) {
					pathFromMeToDestination.cost = receivedPath.cost + pathFromMeToDistanceVectorOwner.cost;
					pathFromMeToDestination.gatewayRouterID = changedVectorRouterID;
					if (pathFromMeToDestination.cost > maxCountToInfinity) {
						pathFromMeToDestination.cost = maxCountToInfinity;
					}
				}
			}
		}

		Set<PathInfo> currentSet = new HashSet<PathInfo>(myDistanceTable.values());
		Set<PathInfo> previousSet = new HashSet<PathInfo>(beforeList.values());
		return !currentSet.containsAll(previousSet) || !previousSet.containsAll(currentSet);
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
