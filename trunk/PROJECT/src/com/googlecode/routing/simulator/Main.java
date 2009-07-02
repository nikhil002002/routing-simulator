package com.googlecode.routing.simulator;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Renato Miceli
 */
public class Main {

	public static final String ENLACES_CONFIG_DIR = "resources/enlaces.config";

	public static final String ROTEADOR_CONFIG_DIR = "resources/roteador.config";

	public static void main(String[] args) throws IOException, InterruptedException {

		if (args.length < 1) {
			System.err.println("USAGE: <router_id>");
			System.exit(1);
		}

		String strID = args[0];

		long id = 0;
		try {
			id = Long.parseLong(strID);
		} catch (NumberFormatException e) {
			System.err.println("The given router ID <" + strID + "> is not a valid long integer!");
			System.exit(2);
		}

		RouterTable routerTable = new RouterTable(ROTEADOR_CONFIG_DIR);
		routerTable.parseConfigFile();

		RouterInfo currentRouterInfo = routerTable.getInfo(id);
		if (currentRouterInfo == null) {
			System.err.println("No configuration was found for the given router ID <" + id + ">!");
			System.exit(3);
		}

		LinkTable linkTable = new LinkTable(ENLACES_CONFIG_DIR);
		linkTable.parseConfigFile();

		Map<Long, LinkInfo> links = linkTable.getLinksForRouter(id);
		Set<RouterInfo> adjacentRouters = new HashSet<RouterInfo>();
		for (LinkInfo info : links.values()) {
			adjacentRouters.add(routerTable.getInfo(info.routerAID == id ? info.routerBID : info.routerAID));
		}

		Router router = new Router(currentRouterInfo, adjacentRouters, links, System.out, linkTable.getCostsSum());
		router.initSocket();

		new Thread(new RouterServer(router)).start();
		new Thread(new RouterClient(router)).start();
	}
}
