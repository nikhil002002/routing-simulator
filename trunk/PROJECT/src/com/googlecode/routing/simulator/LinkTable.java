package com.googlecode.routing.simulator;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Contains the set of information about the links to other routers that are connected to the one
 *  that owns this entity
 *  
 * @author Felipe Ribeiro
 * @author Michelly Guedes
 * @author Renato Miceli
 */
public class LinkTable {

	/**
	 * Path to the plain text file that contains the data that should be loaded initially inside this LinkTable
	 */
	private final String linkConfigFilePath;

	/**
	 * The set of LinkInfos with information about the link to other routers 
	 */
	private final Set<LinkInfo> links;

	/**
	 * Creates a new LinkTable and assigns the path of the text file that contains the data to be loaded
	 * and parsed
	 * 
	 * @param linkConfigFilePath The path to the text file that contains the data to be loaded
	 */
	public LinkTable(String linkConfigFilePath) {
		this.linkConfigFilePath = linkConfigFilePath;
		this.links = new HashSet<LinkInfo>();
	}

	/**
	 * Parses the text file and mounts the table containing the information about the links
	 * 
	 * @throws IOException
	 */
	public void parseConfigFile() throws IOException {
		BufferedReader buf = new BufferedReader(new InputStreamReader(new FileInputStream(linkConfigFilePath)));
		String line;
		while ((line = buf.readLine()) != null) {
			String[] strLine = line.trim().split("\\s+");
			LinkInfo info = new LinkInfo();
			info.routerAID = Long.parseLong(strLine[0]);
			info.routerBID = Long.parseLong(strLine[1]);
			info.cost = Double.parseDouble(strLine[2]);
			links.add(info);
		}
		buf.close();
	}

	/**
	 * Return a list of links that reach the given router
	 * 
	 * @param routerID
	 * @return A map containing all the links that reach the router passed as param
	 */
	public Map<Long, LinkInfo> getLinksForRouter(long routerID) {
		Map<Long, LinkInfo> result = new HashMap<Long, LinkInfo>();
		for (LinkInfo info : links) {
			if (info.routerAID == routerID) {
				result.put(info.routerBID, info);
			} else if (info.routerBID == routerID) {
				result.put(info.routerAID, info);
			}
		}
		return result;
	}

	/**
	 * Calculates the maximum possible distance of two vertices of the graph
	 * 
	 * @return the maximum size of the graph
	 */
	public double getCostsSum() {
		double maxDist = 0;
		for (LinkInfo info : links) {
			maxDist += info.cost;
		}
		return maxDist;
	}

}
