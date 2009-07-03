package com.googlecode.routing.simulator;

/**
 * Contains the information about how to reach another router
 * 
 * @author Felipe Ribeiro
 * @author Michelly Guedes
 * @author Renato Miceli
 */
public class PathInfo {

	/**
	 * The id of the destination router
	 */
	public long destinationRouterID;

	/**
	 * The id of the gateway router (i.e. the one to which I should send the package to reach the 
	 * @link{destinationRouterId} with the lowest known cost
	 */
	public long gatewayRouterID;

	/**
	 * The cost to reach the destinationRouterId from the actual router
	 */
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

	/**
	 * Parses a string to create a PathInfo from a serialized source
	 * @param A string that represents a PathInfo in a serialized way (destinationRouterId:gatewayRouterId:cost)
	 * @return the PathInfo object with the data obtained from the string
	 */
	public static PathInfo buildPathInfo(String s) {
		String[] strLine = s.split(":");
		PathInfo info = new PathInfo();
		info.destinationRouterID = Long.parseLong(strLine[0]);
		info.gatewayRouterID = Long.parseLong(strLine[1]);
		info.cost = Double.parseDouble(strLine[2]);
		return info;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(cost);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + (int) (destinationRouterID ^ (destinationRouterID >>> 32));
		result = prime * result + (int) (gatewayRouterID ^ (gatewayRouterID >>> 32));
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (!PathInfo.class.isInstance(obj)) {
			return false;
		}
		PathInfo path = (PathInfo) obj;
		return destinationRouterID == path.destinationRouterID && gatewayRouterID == path.gatewayRouterID && cost == path.cost;
	}

}
