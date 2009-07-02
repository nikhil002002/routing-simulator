package com.googlecode.routing.simulator;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Map.Entry;

/**
 * @author Renato Miceli
 */
public class RouterClient implements Runnable {

	private final Router router;

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
				this.sendDistanceVectorToNeighbors();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.checkNeighborsTimeout();

			try {
				Thread.sleep(Router.SLEEP_TIME);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void sendDistanceVectorToNeighbors() throws IOException {

		byte[] byteMap;
		synchronized (router.minimumPathTable) {
			byteMap = Router.serialize(router.minimumPathTable);
		}
		for (RouterInfo routerInfo : router.adjacentRouters) {
			DatagramPacket sendPacket = new DatagramPacket(byteMap, byteMap.length, routerInfo.ipAddress, routerInfo.port);
			System.out.println("Enviando para " + routerInfo.ipAddress + ":" + routerInfo.port);
			router.serverSocket.send(sendPacket);
		}
	}

	private void checkNeighborsTimeout() {

		long currentTime = System.currentTimeMillis();
		synchronized (router.lastPing) {
			for (Entry<Long, Long> e : router.lastPing.entrySet()) {
				if (currentTime - e.getValue() > 5 * Router.SLEEP_TIME && router.minimumPathTable.get(e.getKey()).cost != Router.UNAVAILABLE) {
					router.minimumPathTable.get(e.getKey()).cost = Router.UNAVAILABLE;
					System.out.println("[" + router.routerInfo.id + "] Timeout para resposta do roteador [" + e.getKey()
							+ "] atingido, marcando como indisponivel");
				}
			}
		}
	}

}
