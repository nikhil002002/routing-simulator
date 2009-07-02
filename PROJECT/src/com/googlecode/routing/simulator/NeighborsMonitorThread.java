package com.googlecode.routing.simulator;

public class NeighborsMonitorThread extends Thread {
	
	private final Router router;

	public NeighborsMonitorThread(Router router) {
		this.router = router;
	}
	
	public void run() {
		while(true) {
			try {
				router.verifyNeighbors();
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
