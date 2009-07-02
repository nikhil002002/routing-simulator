/**
 * 
 */
package com.googlecode.routing.simulator;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Renato Miceli
 */
public class LinkTable {

	private final String linkConfigFilePath;

	private final Set<LinkInfo> links;

	public LinkTable(String linkConfigFilePath) {
		this.linkConfigFilePath = linkConfigFilePath;
		this.links = new HashSet<LinkInfo>();
	}

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

	public Set<LinkInfo> getLinksForRouter(long routerID) {
		Set<LinkInfo> result = new HashSet<LinkInfo>();
		for (LinkInfo info : links) {
			if (info.routerAID == routerID || info.routerBID == routerID) {
				result.add(info);
			}
		}
		return result;
	}
	
	public double getMaxCost() {
		double dist = 0;
		for(LinkInfo link : links) {
			if(link.cost > dist) {
				dist = link.cost;
			}
		}
		return dist;
	}

}
