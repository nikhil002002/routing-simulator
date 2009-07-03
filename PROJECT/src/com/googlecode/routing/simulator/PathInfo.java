package com.googlecode.routing.simulator;

/**
 * @author Renato Miceli
 */
public class PathInfo {

	public long destinationRouterID;

	public long gatewayRouterID;

	public double cost;
	
	public PathInfo() {
		
	}
	
	public PathInfo(PathInfo info) {
		this.destinationRouterID = info.destinationRouterID;
		this.gatewayRouterID = info.gatewayRouterID;
		this.cost = info.cost;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return destinationRouterID + ":" + gatewayRouterID + ":" + cost;
	}

	public static PathInfo buildPathInfo(String s) {
		String[] strLine = s.split(":");
		PathInfo info = new PathInfo();
		info.destinationRouterID = Long.parseLong(strLine[0]);
		info.gatewayRouterID = Long.parseLong(strLine[1]);
		info.cost = Double.parseDouble(strLine[2]);
		return info;
	}

}
