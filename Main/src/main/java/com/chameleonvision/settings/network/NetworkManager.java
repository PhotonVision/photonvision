package com.chameleonvision.settings.network;


import com.chameleonvision.settings.Platform;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class NetworkManager {
	private NetworkManager() {}

	private static INetworking networking;

	public static void init() {
		Platform platform = Platform.getCurrentPlatform();

		if (platform.isLinux()) {
			networking = new LinuxNetworking();
		} else if (platform.isWindows()) {
			networking = new WindowsNetworking();
		}

		List<NetworkInterface> interfaces = new ArrayList<>();

		try {
			interfaces = networking.getNetworkInterfaces();
		} catch (SocketException e) {
			e.printStackTrace();
		}

		if (interfaces != null) {
			for (var inetface : interfaces) {
				if (inetface.displayName.toLowerCase().contains("asus")) {
//					networking.setHostname("BIGRIG");
				}
			}
		}
	}
}
