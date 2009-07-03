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
						newInfo.gatewayRouterID = changedVectorRouterID;
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
