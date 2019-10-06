package com.chameleonvision.network;

import com.chameleonvision.settings.NetworkSettings;
import com.chameleonvision.settings.SettingsManager;
import com.chameleonvision.util.ShellExec;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LinuxNetworking extends SysNetworking {

	private ShellExec shell = new ShellExec(true, true);

	@Override
	public boolean setDHCP() {
		var ifaceName = networkInterface.name;
		var ethResetCmd = String.format("ifconfig %s 0.0.0.0 0.0.0.0", ifaceName);
		var dhclientCmd = String.format("dhclient %s", ifaceName);


		// ifconfig eth0 0.0.0.0 0.0.0.0
		try {
			int retCode = shell.execute("ifconfig", null, true, ifaceName, "0.0.0.0", "0.0.0.0");
			while (!shell.isOutputCompleted() && !shell.isErrorCompleted()) {}
			var out = shell.getOutput();
			var err = shell.getError();
			if (retCode != 0) return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		try {
			int retCode = shell.execute("dhclient", null, true, ifaceName);
			while (!shell.isOutputCompleted() && !shell.isErrorCompleted()) {}
			var out = shell.getOutput();
			var err = shell.getError();
			if (retCode != 0) return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	@Override
	public boolean setHostname(String newHostname) {
		var cmdString = String.format("hostnamectl set-hostname %s", newHostname);

		try {
			var process = Runtime.getRuntime().exec(cmdString);
			var returnCode = shell.execute("hostnamectl", null, true, "set-hostname", newHostname);
			return returnCode == 0;
		} catch(Exception e)  {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean setStatic(String ipAddress, String netmask, String gateway, String broadcast) {
		try {
			int clearRetCode = shell.execute("ip addr flush dev", null, true, networkInterface.name);
			int setIPRetCode = shell.execute(String.format("ip addr add %s/%s broadcast %s dev %s", ipAddress, netmask, broadcast, networkInterface.name), null, true);
			int setGatewayRetCode = shell.execute(String.format("ip route replace default via %s dev %s", gateway, networkInterface.name), null, false);
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
