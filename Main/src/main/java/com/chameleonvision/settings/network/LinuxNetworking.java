package com.chameleonvision.settings.network;

import java.net.SocketException;
import java.util.List;

public class LinuxNetworking implements INetworking {

	@Override
	public String getHostname() {
		return null;
	}

	@Override
	public NetworkIPMode getIPMode() {
		return null;
	}

	@Override
	public boolean setDHCP() {
		return false;
	}

	@Override
	public boolean setHostname(String hostname) {
		return false;
	}

	@Override
	public boolean setStatic(String ipAddress, String netmask, String gateway) {
		return false;
	}

	@Override
	public List<NetworkInterface> getNetworkInterfaces() throws SocketException {
		return null;
	}
}
