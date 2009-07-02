package com.googlecode.routing.simulator;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Renato Miceli
 */
public class RouterTable {

	private final String routerConfigFilePath;

	private final Map<Long, RouterInfo> routers;

	public RouterTable(String routerConfigFilePath) {
		this.routerConfigFilePath = routerConfigFilePath;
		this.routers = new HashMap<Long, RouterInfo>();
	}

	public void parseConfigFile() throws IOException {
		BufferedReader buf = new BufferedReader(new InputStreamReader(new FileInputStream(routerConfigFilePath)));
		String line;
		while ((line = buf.readLine()) != null) {
			String[] strLine = line.trim().split("\\s+");
			RouterInfo info = new RouterInfo();
			info.id = Long.parseLong(strLine[0]);
			info.port = Integer.parseInt(strLine[1]);

			String[] split = strLine[2].split("\\.");
			byte[] bytedIP = new byte[4];
			for (int i = 0; i < split.length; i++) {
				bytedIP[i] = (byte) Integer.parseInt(split[i]);
			}
			info.ipAddress = InetAddress.getByAddress(bytedIP);
			routers.put(info.id, info);
		}
		buf.close();
	}

	public RouterInfo getInfo(long id) {
		return routers.get(id);
	}
	
	public int getNumberOfRouters() {
		return routers.size();
	}

}
