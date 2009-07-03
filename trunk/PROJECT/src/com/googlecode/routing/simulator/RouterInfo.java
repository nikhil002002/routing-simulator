package com.googlecode.routing.simulator;

import java.net.InetAddress;

/**
 * Metadata about a router
 *
 * @author Felipe Ribeiro
 * @author Michelly Guedes
 * @author Renato Miceli
 */
public class RouterInfo {
	
	/**
	 * the identifier of the router
	 */
	public long id;

	/**
	 * The port that it uses to send/receive messages
	 */
	public int port;

	/**
	 * The IP address where it's running
	 */
	public InetAddress ipAddress;

}