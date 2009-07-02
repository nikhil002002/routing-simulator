package com.googlecode.routing.simulator;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Map;

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

			synchronized (router.minimumPathTable) {
				router.minimumPathTable.put(info.id, receivedMap);
			}

			boolean changed = relaxEdges(info.id);
			
			updatePingTable(info.id);

			// //////////////////////////////////////////////////////////////////////////////////////

			// PathInfo pathToSender;
			// synchronized (router.minimumPathTable) {
			// if (!router.minimumPathTable.containsKey(info.id)) {
			// pathToSender = new PathInfo();
			// pathToSender.destinationRouterID = info.id;
			// pathToSender.gatewayRouterID = info.id;
			// double thisCost = Router.UNAVAILABLE;
			// for (LinkInfo linkInfo : router.links) {
			// if (linkInfo.routerAID == router.routerInfo.id ||
			// linkInfo.routerBID == router.routerInfo.id) {
			// thisCost = linkInfo.cost;
			// break;
			// }
			// }
			// pathToSender.cost = Math.min(thisCost,
			// receivedMap.get(router.routerInfo.id).cost);
			// router.minimumPathTable.put(info.id, pathToSender);
			// } else {
			// pathToSender = router.minimumPathTable.get(info.id);
			// pathToSender.cost = Math.min(pathToSender.cost,
			// receivedMap.get(router.routerInfo.id).cost);
			// }
			// }
			//
			// double cost = pathToSender.cost;
			//
			// updatePingTable(info.id);
			//
			// boolean changed = false;
			// for (Entry<Long, PathInfo> entry : receivedMap.entrySet()) {
			//
			// long id = entry.getKey();
			// PathInfo receivedPathInfo = entry.getValue();
			//
			// if (receivedPathInfo.destinationRouterID == router.routerInfo.id)
			// {
			// continue;
			// }
			//
			// PathInfo actualPathInfo;
			// synchronized (router.minimumPathTable) {
			// actualPathInfo = router.minimumPathTable.get(id);
			// }
			//
			// if (actualPathInfo == null) {
			// actualPathInfo = new PathInfo();
			// actualPathInfo.destinationRouterID =
			// receivedPathInfo.destinationRouterID;
			// actualPathInfo.cost = receivedPathInfo.cost == Router.UNAVAILABLE
			// ? Router.UNAVAILABLE : receivedPathInfo.cost + cost;
			// actualPathInfo.gatewayRouterID = info.id;
			// synchronized (router.minimumPathTable) {
			// router.minimumPathTable.put(id, actualPathInfo);
			// }
			// changed = true;
			// router.out.println("[" + router.routerInfo.id +
			// "] Acrescentei nó [" + receivedPathInfo.destinationRouterID +
			// "]");
			// } else if (receivedPathInfo.cost == Router.UNAVAILABLE &&
			// actualPathInfo.cost != Router.UNAVAILABLE
			// && (receivedPathInfo.gatewayRouterID ==
			// actualPathInfo.gatewayRouterID || actualPathInfo.gatewayRouterID
			// == info.id)) {
			// router.out.println("[" + router.routerInfo.id + "] RECEBI QUE ["
			// + id + "] caiu");
			// actualPathInfo.cost = Router.UNAVAILABLE;
			// changed = true;
			//
			// } else if (receivedPathInfo.cost + cost < actualPathInfo.cost) {
			//
			// double previousCost = actualPathInfo.cost;
			// actualPathInfo.cost = receivedPathInfo.cost + cost;
			// actualPathInfo.gatewayRouterID = info.id;
			//
			// router.out.println("[" + router.routerInfo.id +
			// "]: Houveram mudanças na minha tabela ");
			// router.out.println("[" + router.routerInfo.id +
			// "]: O custo para " + actualPathInfo.destinationRouterID + " era "
			// + (previousCost == Router.UNAVAILABLE ? "N/A" : previousCost) +
			// " e agora eh " + (actualPathInfo.cost));
			//
			// changed = true;
			// }
			// }

			if (changed) {
				router.printDistanceTable();
			}
		}

	}

	private boolean relaxEdges(long changedVectorRouterID) {
		boolean changed = false;

		synchronized (router.minimumPathTable) {
			Map<Long, PathInfo> receivedMap = router.minimumPathTable.get(changedVectorRouterID);
			Map<Long, PathInfo> myDistanceTable = router.getDistanceTable();

			PathInfo pathToAdjacent = myDistanceTable.get(changedVectorRouterID);
			if (pathToAdjacent.cost == Router.UNAVAILABLE) {
				pathToAdjacent.cost = router.links.get(changedVectorRouterID).cost;
			}

			for (PathInfo receivedInfo : receivedMap.values()) {
				if (!myDistanceTable.containsKey(receivedInfo.destinationRouterID)) {
					PathInfo newInfo = new PathInfo();
					newInfo.destinationRouterID = receivedInfo.destinationRouterID;
					newInfo.gatewayRouterID = changedVectorRouterID;
					newInfo.cost = receivedInfo.cost + pathToAdjacent.cost;
					myDistanceTable.put(newInfo.destinationRouterID, newInfo);
					changed = true;
				} else {
					PathInfo newInfo = myDistanceTable.get(receivedInfo.destinationRouterID);
					if (newInfo.cost > receivedInfo.cost + pathToAdjacent.cost) {
						newInfo.cost = receivedInfo.cost + pathToAdjacent.cost;
						changed = true;
					}
				}
			}
		}
		return changed;
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

}
