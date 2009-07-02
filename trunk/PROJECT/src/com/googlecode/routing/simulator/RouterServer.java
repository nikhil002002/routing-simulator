package com.googlecode.routing.simulator;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Renato Miceli
 */
public class RouterServer implements Runnable {

	private final Router router;

	public RouterServer(Router router) {
		this.router = router;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run() {

		while (true) {

			DatagramPacket receivedPacket;
			try {
				receivedPacket = receiveData();
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}

			Map<Long, PathInfo> receivedMap = Router.deserialize(receivedPacket.getData());

			RouterInfo info = router.getAdjacentByIPAndPort(receivedPacket.getAddress(), receivedPacket.getPort());
			if (info == null) {
				continue;
			}

			PathInfo pathToSender;
			synchronized (router.minimumPathTable) {
				if (!router.minimumPathTable.containsKey(info.id)) {
					pathToSender = new PathInfo();
					pathToSender.destinationRouterID = info.id;
					pathToSender.gatewayRouterID = info.id;
					// FIXME: the minimum distance to reach me might not be
					// through the direct link
					pathToSender.cost = receivedMap.get(router.routerInfo.id).cost;
					router.minimumPathTable.put(info.id, pathToSender);
				} else {
					pathToSender = router.minimumPathTable.get(info.id);
				}
			}

			// cost of this link
			double cost = pathToSender.cost;

			System.out.println("[" + router.routerInfo.id + "]: Recebi de " + info.id);

			updatePingTable(info.id);

			for (Long id : receivedMap.keySet()) {

				PathInfo receivedPathInfo = receivedMap.get(id);
				PathInfo actualPathInfo = router.minimumPathTable.get(id);

				// If the target router is this one
				if (receivedPathInfo.destinationRouterID == router.routerInfo.id) {

					// If the other router thinks I'm off, disconsider
					if (receivedPathInfo.cost == Router.UNAVAILABLE)
						continue;

					// reverse to have the sender as the destination router
					receivedPathInfo.destinationRouterID = info.id;
					// reverse to have the sender as the gateway
					receivedPathInfo.gatewayRouterID = info.id;
				}

				// If i don't have a link to this node, or the availability is
				// different from what i have or the cost is lower
				if (actualPathInfo == null || receivedPathInfo.cost == Router.UNAVAILABLE ^ actualPathInfo.cost == Router.UNAVAILABLE
						|| receivedPathInfo.cost + cost < actualPathInfo.cost) {

					// If i don't have a link
					if (actualPathInfo == null) {
						actualPathInfo = new PathInfo();
						actualPathInfo.destinationRouterID = receivedPathInfo.destinationRouterID;
						actualPathInfo.cost = receivedPathInfo.cost;
					}
					double previousCost = actualPathInfo.cost;

					if (receivedPathInfo.cost == Router.UNAVAILABLE && actualPathInfo.cost != Router.UNAVAILABLE) {
						actualPathInfo.cost = Router.UNAVAILABLE;
						System.out.println("Fui avisado pelo roteador [" + info.id + "] que o roteador [" + actualPathInfo.destinationRouterID
								+ "] estah indisponivel");
					} else if (actualPathInfo.cost != Router.UNAVAILABLE) {
						actualPathInfo.cost = receivedPathInfo.cost + cost;
					}

					if (previousCost != actualPathInfo.cost) {
						System.out.println("[" + router.routerInfo.id + "]: Houveram mudanças na minha tabela ");
						System.out.println("[" + router.routerInfo.id + "]: O custo para " + actualPathInfo.destinationRouterID + " era "
								+ previousCost + " e agora eh " + (actualPathInfo.cost));

					}
					actualPathInfo.gatewayRouterID = info.id;

					router.minimumPathTable.put(id, actualPathInfo);
					printTable();
				}
			}
		}
	}

	private DatagramPacket receiveData() throws IOException {
		byte[] receiveData = new byte[1024];
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		router.serverSocket.receive(receivePacket);
		return receivePacket;
	}

	private void updatePingTable(long routerID) {
		synchronized (router.lastPing) {
			router.lastPing.put(routerID, System.currentTimeMillis());
		}
	}

	private void printTable() {
		System.out.println("| ID |  GATEWAY |  COST  |");
		for (Entry<Long, PathInfo> entry : router.minimumPathTable.entrySet()) {
			System.out.println("| " + entry.getKey() + " |  " + entry.getValue().gatewayRouterID + " |  " + entry.getValue().cost + "  |");
		}
		System.out.println();
	}

}
