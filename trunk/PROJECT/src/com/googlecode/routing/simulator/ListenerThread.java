package com.googlecode.routing.simulator;

import java.io.IOException;

public class ListenerThread extends Thread {
	
	private final Router router;

	public ListenerThread(Router router) {
		this.router = router;
		
	}
	
	public void run() {
		try {
			while(true) {
				router.listen();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
