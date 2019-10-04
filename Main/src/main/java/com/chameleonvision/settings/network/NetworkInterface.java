package com.chameleonvision.settings.network;

import com.chameleonvision.settings.NetworkSettings;
import com.chameleonvision.settings.SettingsManager;

import java.net.InetAddress;
import java.util.Collections;

public class NetworkInterface {
	public final String name;
	public final String displayName;
//	public NetworkIPMode IPMode;
//	public String IPAddress;
//	public String Netmask;
//	public String Gateway;

	public NetworkInterface(java.net.NetworkInterface inetface) {

		name = inetface.getName();
		displayName = inetface.getDisplayName();

	}
}
