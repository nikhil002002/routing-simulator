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

	public double getCostsSum() {
		double maxDist = 0;
		for (LinkInfo info : links) {
			maxDist += info.cost;
		}
		return maxDist;
	}

}
