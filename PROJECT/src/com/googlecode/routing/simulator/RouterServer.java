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
					double thisCost = Router.UNAVAILABLE;
					for (LinkInfo linkInfo : router.links) {
						if (linkInfo.routerAID == router.routerInfo.id || linkInfo.routerBID == router.routerInfo.id) {
							thisCost = linkInfo.cost;
							break;
						}
					}
					pathToSender.cost = Math.min(thisCost, receivedMap.get(router.routerInfo.id).cost);
					router.minimumPathTable.put(info.id, pathToSender);
				} else {
					pathToSender = router.minimumPathTable.get(info.id);
				}
			}

			double cost = pathToSender.cost;

			router.out.println("[" + router.routerInfo.id + "]: Recebi de " + info.id);

			updatePingTable(info.id);

			boolean changed = false;
			for (Entry<Long, PathInfo> entry : receivedMap.entrySet()) {

				long id = entry.getKey();
				PathInfo receivedPathInfo = entry.getValue();

				if (receivedPathInfo.destinationRouterID == router.routerInfo.id) {
					continue;
				}
				
				PathInfo actualPathInfo;
				synchronized (router.minimumPathTable) {
					actualPathInfo = router.minimumPathTable.get(id);
				}
				
				if(actualPathInfo == null || (cost + receivedPathInfo.cost != actualPathInfo.cost && receivedPathInfo.cost != actualPathInfo.cost)) {
					System.out.print("RECEBI QUE ["+ id+ "] AGORA TEM CUSTO "+	(cost + receivedPathInfo.cost)+", EU TINHA COMO ");
					if(actualPathInfo==null) System.out.println("NULL");
					else System.out.println(actualPathInfo.cost);
				}
				
				if (actualPathInfo == null) {
					actualPathInfo = new PathInfo();
					actualPathInfo.destinationRouterID = receivedPathInfo.destinationRouterID;
					actualPathInfo.cost = receivedPathInfo.cost == Router.UNAVAILABLE? Router.UNAVAILABLE : receivedPathInfo.cost + cost;
					actualPathInfo.gatewayRouterID = info.id;
					synchronized (router.minimumPathTable) {
						router.minimumPathTable.put(id, actualPathInfo);
					}
					changed = true;
					System.out.println("Acrescentei nó ["+ receivedPathInfo.destinationRouterID+ "]");
				} else if(receivedPathInfo.cost == Router.UNAVAILABLE && actualPathInfo.cost != Router.UNAVAILABLE) {
					System.out.println("RECEBI QUE ["+ id+ "] caiu");
					actualPathInfo.cost = Router.UNAVAILABLE;
					changed = true;
					
				} else if (receivedPathInfo.cost + cost < actualPathInfo.cost) {

					double previousCost = actualPathInfo.cost;
					actualPathInfo.cost = receivedPathInfo.cost + cost;
					actualPathInfo.gatewayRouterID = info.id;

					router.out.println("[" + router.routerInfo.id + "]: Houveram mudan�as na minha tabela ");
					router.out.println("[" + router.routerInfo.id + "]: O custo para " + actualPathInfo.destinationRouterID + " era " + previousCost
							+ " e agora eh " + (actualPathInfo.cost));

					changed = true;
				} 
			}

			if (changed) {
				router.printDistanceTable();
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

}
