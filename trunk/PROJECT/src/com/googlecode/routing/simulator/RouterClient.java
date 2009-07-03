package com.googlecode.routing.simulator;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Implements the client side of the router that sends messages to other routers
 * that are directly connected to this one
 *  
 * @author Felipe Ribeiro
 * @author Michelly Guedes
 * @author Renato Miceli
 */
public class RouterClient implements Runnable {

	/**
	 * The router that owns this entity
	 */
	private final Router router;

	/**
	 * Initialize the client attributing an owner
	 * @param router
	 */
	public RouterClient(Router router) {
		this.router = router;

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run() {
		while (true) {

			try {
				this.checkNeighborsTimeout();
				this.sendDistanceVectorToNeighbors();
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				Thread.sleep(Router.SLEEP_TIME);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Sends the table of distance to the neighbors
	 * @throws IOException
	 */
	private void sendDistanceVectorToNeighbors() throws IOException {

		byte[] byteMap;
		synchronized (router.minimumPathTable) {
			byteMap = Router.serialize(router.getDistanceTable());
		}
		for (RouterInfo routerInfo : router.adjacentRouters) {
			DatagramPacket sendPacket = new DatagramPacket(byteMap, byteMap.length, routerInfo.ipAddress, routerInfo.port);
			router.serverSocket.send(sendPacket);
		}
	}

	/**
	 * Checks if a neighbor has got in touch before the timeout, if not, mark as unavailable
	 * 
	 * @throws IOException
	 */
	private void checkNeighborsTimeout() throws IOException {

		long currentTime = System.currentTimeMillis();

		Set<Entry<Long, Long>> set;
		synchronized (router.lastPing) {
			set = new HashSet<Entry<Long, Long>>(router.lastPing.entrySet());
		}

		boolean changed = false;
		synchronized (router.minimumPathTable) {
			Map<Long, PathInfo> myDistanceVector = router.getDistanceTable();
			for (Entry<Long, Long> e : set) {
				if (e.getValue() > 0 && currentTime - e.getValue() > 5 * Router.SLEEP_TIME) {
					Long id = e.getKey();
					if (myDistanceVector.get(id).cost != Router.INFINITY) {
						myDistanceVector.get(id).cost = Router.INFINITY;
						e.setValue(0L);

						for (PathInfo info : myDistanceVector.values()) {
							if (info.gatewayRouterID == id) {
								info.gatewayRouterID = -1;
								info.cost = Router.INFINITY;
							}
						}

						for(PathInfo path : router.minimumPathTable.get(id).values()) {
							path.cost = Router.INFINITY;
							path.gatewayRouterID = -1;
						}

						for (Long entry : router.minimumPathTable.keySet()) {
							router.relaxEdges(entry);
						}

						router.out.println("[" + router.routerInfo.id + "] Timeout para resposta do roteador [" + id
								+ "] atingido, marcando como indisponivel");

						changed = true;
					}
				}
			}
		}

		if (changed) {
			router.printDistanceTable();
		}
	}
}
