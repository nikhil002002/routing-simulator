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
 * Represents the Router and calls the actions to communicate and receive messages from other Routers
 * 
 * @author Felipe Ribeiro
 * @author Michelly Guedes
 * @author Renato Miceli
 */
public class Router {

	/**
	 * The interval between sent messages containing the table of links
	 */
	public static final long SLEEP_TIME = 4000;
	
	/**
	 * A map that contains the minimum known path to any know node of the graph
	 */
	public final Map<Long, Map<Long, PathInfo>> minimumPathTable;
	
	/**
	 * The metadata of the router
	 */
	public final RouterInfo routerInfo;
	
	/**
	 * The known links to other routers
	 */
	public final Map<Long, LinkInfo> links;
	
	/**
	 * The set of routers that are directly connected to this one
	 */
	public final Set<RouterInfo> adjacentRouters;
	
	/**
	 * The map that contains the timestamp of the last message of each adjacent router.
	 * Useful to identify disconnections
	 */
	public Map<Long, Long> lastPing;
	
	/**
	 * Maximum value to consider on count to infinity
	 */
	public final double maxCountToInfinity;
	
	/**
	 * Abstraction of infinity
	 */
	public static final double INFINITY = Double.MAX_VALUE;

	/**
	 * Stream used to output messages
	 */
	public final PrintStream out;

	/**
	 * Socket that is used to send and receive messages
	 */
	public DatagramSocket serverSocket;

	/**
	 * Initializes the Router
	 * @param routerInfo Metadata about the router
	 * @param adjacentRouters Set of directly connected routers
	 * @param links Map containing the data used to connect to any known node of the graph
	 * @param out The output entity
	 * @param maxCountToInfinity
	 */
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

	/**
	 * Returns the map containing the minimum known distance to each known node
	 * @return
	 */
	public Map<Long, PathInfo> getDistanceTable() {
		return this.minimumPathTable.get(this.routerInfo.id);
	}

	/**
	 * Initializes the socket
	 *  
	 * @throws SocketException
	 * @throws UnknownHostException
	 */
	public void initSocket() throws SocketException, UnknownHostException {
		serverSocket = new DatagramSocket(routerInfo.port, routerInfo.ipAddress);
		System.out.println("Ouvindo em: " + routerInfo.ipAddress + ":" + routerInfo.port);
	}

	/**
	 * Based on the ip address and port used, this method can identify what is the ID of the router
	 * @param inetAddr
	 * @param port
	 * @return the complete metadata of the router
	 */
	public RouterInfo getAdjacentByIPAndPort(InetAddress inetAddr, int port) {
		for (RouterInfo info : adjacentRouters) {
			if (info.ipAddress.equals(inetAddr) && info.port == port) {
				return info;
			}
		}
		return null;
	}

	/**
	 * Attributes new weights to the edges
	 * 
	 * @param changedVectorRouterID
	 * @return if there was any information that wasn't already registered
	 */
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

	/**
	 * Outputs the table of dustances
	 */
	public void printDistanceTable() {
		out.println("| ID |  COST  |  GATEWAY |");
		for (PathInfo info : minimumPathTable.get(routerInfo.id).values()) {
			out.println("|  " + info.destinationRouterID + " |   " + (info.cost == INFINITY ? "N/A" : info.cost) + "  |     " + info.gatewayRouterID
					+ "    |");
		}
		out.println();
	}

	/**
	 * Serializes the table of distances to send through the network
	 * 
	 * @param map
	 * @return the serialized data
	 */
	public static byte[] serialize(Map<Long, PathInfo> map) {
		StringBuilder bld = new StringBuilder();
		for (PathInfo entry : map.values()) {
			bld.append(entry.toString() + "\n");
		}
		String result = bld.toString();
		return result.substring(0, result.length() - 1).getBytes();
	}

	/**
	 * Deserialize the data and mounts a table of distances
	 * 
	 * @param array
	 * @return the table of distances
	 */
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
