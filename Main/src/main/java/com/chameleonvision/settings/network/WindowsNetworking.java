package com.chameleonvision.settings.network;

import com.chameleonvision.settings.NetworkSettings;
import com.chameleonvision.settings.SettingsManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WindowsNetworking implements INetworking {

	@Override
	public String getHostname() {
		try {
			InetAddress localhost = InetAddress.getLocalHost();
			return localhost.getHostName().split("/")[0];
		} catch (UnknownHostException e) {
			return null;
		}
	}

	@Override
	public NetworkIPMode getIPMode() {
		return NetworkIPMode.UNKNOWN;
	}

	@Override
	public boolean setDHCP() {
		return false;
	}

	@Override
	public boolean setHostname(String newHostname) {
		var currentHostname = getHostname();

		if (getHostname() == null) {
			return false;
		}

		String command = String.format("wmic computersystem where name=\"%s\" call rename name=\"%s\"", currentHostname, newHostname);

		try {
			var process = Runtime.getRuntime().exec(command);
			var returnCode = process.waitFor();
			return returnCode == 0;
		} catch(Exception e)  {
			return false;
		}
	}

	@Override
	public boolean setStatic(String ipAddress, String netmask, String gateway) {
		return false;
	}

	@Override
	public List<NetworkInterface> getNetworkInterfaces() throws SocketException {
		var netInterfaces = Collections.list(java.net.NetworkInterface.getNetworkInterfaces());

		List<NetworkInterface> goodInterfaces = new ArrayList<>();

		var teamBytes = NetworkSettings.GetTeamNumberIPBytes(SettingsManager.GeneralSettings.team_number);

		for (var inetface : netInterfaces) {
			if (inetface.getDisplayName().toLowerCase().contains("bluetooth")) continue;
			if (inetface.getDisplayName().toLowerCase().contains("virtual")) continue;
			if (inetface.getDisplayName().toLowerCase().contains("loopback")) continue;
			if (!inetface.isUp()) continue;
			for (var inetAddr : Collections.list(inetface.getInetAddresses())) {
				var rawAddr = inetAddr.getAddress();
				if (rawAddr[1] == teamBytes[0] && rawAddr[2] == teamBytes[1]) {
					goodInterfaces.add(new NetworkInterface(inetface));
				}
			}
		}
		return goodInterfaces;
	}
}
