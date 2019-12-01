package com.chameleonvision.network;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LinuxNetworking extends SysNetworking {

	@Override
	public boolean setDHCP() {
		String[] clearArgs = { "addr", "flush", "dev", networkInterface.name };
		try {
			int clearRetCode = shell.execute("ip", clearArgs);
			int dhcpRetCode = shell.execute("dhclient", networkInterface.name);
			return clearRetCode == 0 && dhcpRetCode == 0;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean setHostname(String newHostname) {
		String[] setHostnameArgs = { "set-hostname", newHostname };
		try {
			var setHostnameRetCode = shell.execute("hostnamectl", setHostnameArgs);
			return setHostnameRetCode == 0;
		} catch(Exception e)  {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean setStatic(String ipAddress, String netmask, String gateway, String broadcast) {
		try {
			String[] clearArgs = { "addr", "flush", "dev", networkInterface.name };
			String[] setIPArgs = { "addr", "add", String.format("%s/%s", ipAddress, netmask), "broadcast", broadcast, "dev", networkInterface.name };
			String[] setGatewayArgs = { "route", "replace", "default", "via", gateway, "dev", networkInterface.name };

			int clearRetCode = shell.execute("ip", clearArgs);
			int setIPRetCode = shell.execute("ip", setIPArgs);
			int setGatewayRetCode = shell.execute("ip", setGatewayArgs);

			return clearRetCode == 0 && setIPRetCode == 0 && setGatewayRetCode == 0;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public List<java.net.NetworkInterface> getNetworkInterfaces() throws SocketException {
		List<java.net.NetworkInterface> netInterfaces;
		try {
			netInterfaces = Collections.list(java.net.NetworkInterface.getNetworkInterfaces());
		} catch (SocketException e) {
			return null;
		}

		List<java.net.NetworkInterface> goodInterfaces = new ArrayList<>();

		for (var netInterface : netInterfaces) {
		    if (netInterface.getDisplayName().contains("lo")) continue;
			if (!netInterface.isUp()) continue;
			goodInterfaces.add(netInterface);
		}
		return goodInterfaces;

	}
}
