package com.chameleonvision.settings.network;

import java.net.SocketException;
import java.util.List;

public interface INetworking {
	String getHostname();
	NetworkIPMode getIPMode();
	boolean setDHCP();
	boolean setHostname(String hostname);
	boolean setStatic(String ipAddress, String netmask, String gateway);
	List<NetworkInterface> getNetworkInterfaces() throws SocketException;

}
