package com.googlecode.routing.simulator;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Map;

/**
 * Implements the routine of the router that receives messages from the neighbors and update the local information
 * 
 * @author Felipe Ribeiro
 * @author Michelly Guedes
 * @author Renato Miceli
 */
public class RouterServer implements Runnable {

	/**
	 * The router that owns this entity
	 */
	private final Router router;

	/**
	 * Initializes the entity for the given Router
	 * 
	 * @param router
	 */
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

			updatePingTable(info.id);

			synchronized (router.minimumPathTable) {
				router.minimumPathTable.put(info.id, receivedMap);
			}

			boolean changed;
			synchronized (router.minimumPathTable) {
				changed = router.relaxEdges(info.id);
			}

			if (changed) {
				router.printDistanceTable();
			}
		}

	}

	/**
	 * Listen to the port and capture the packets
	 * 
	 * @return A network packet
	 * @throws IOException
	 */
	private DatagramPacket receiveData() throws IOException {
		byte[] receiveData = new byte[1024];
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		router.serverSocket.receive(receivePacket);
		return receivePacket;
	}

	/**
	 * Updates the timestamp of the last message from a router
	 * 
	 * @param routerID
	 */
	private void updatePingTable(long routerID) {
		synchronized (router.lastPing) {
			router.lastPing.put(routerID, System.currentTimeMillis());
		}
	}

}
