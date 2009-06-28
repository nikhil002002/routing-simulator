package com.googlecode.routing.simulator;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Renato Miceli
 */
public class Router {

	private final Map<Long, PathInfo> minimumPathTable;
	private final long id;
	private final Set<LinkInfo> links;
	private final Set<RouterInfo> adjacentRouters;

	public Router(long id, Set<RouterInfo> adjacentRouters, Set<LinkInfo> links) {

		this.id = id;
		this.adjacentRouters = adjacentRouters;
		this.links = links;
		this.minimumPathTable = new LinkedHashMap<Long, PathInfo>();

		for (LinkInfo info : links) {
			PathInfo path = new PathInfo();
			path.cost = info.cost;
			if (info.routerAID == id) {
				path.destinationRouterID = info.routerBID;
			} else {
				path.destinationRouterID = info.routerAID;
			}
			path.gatewayRouterID = path.destinationRouterID;
			minimumPathTable.put(path.destinationRouterID, path);
		}

	}
	
	public void startRouting() {
		
	}

}
