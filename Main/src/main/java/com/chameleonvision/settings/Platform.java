package com.chameleonvision.settings;

public enum Platform {
	WINDOWS_64("Windows x64"),
	LINUX_64("Linux x64"),
	LINUX_RASPBIAN("Linux Raspbian"),
	LINUX_AARCH64("Linux ARM 64bit"),
	MACOS_64("Mac OS x64"),
	UNSUPPORTED("Unsupported Platform");

	public final String value;

	Platform(String value) {
		this.value = value;
	}

	public boolean isWindows() {
		return this == WINDOWS_64;
	}

	public boolean isLinux() {
		return this == LINUX_64 || this == LINUX_RASPBIAN || this == LINUX_AARCH64;
	}

	public boolean isMac() {
		return this == MACOS_64;
	}

	public static Platform getCurrentPlatform() {
		var osName = System.getProperty("os.name");
		var osArch = System.getProperty("os.arch");

		if (osName.contains("Windows")) {
			if (osArch.equals("amd64")) return Platform.WINDOWS_64;
			return Platform.UNSUPPORTED;
		}

		if (osName.contains("Linux")) {
			if (osArch.equals("amd64")) return Platform.LINUX_64;
			if (osArch.contains("rasp")) return Platform.LINUX_RASPBIAN;
			if (osArch.contains("aarch")) return Platform.LINUX_64;
			return Platform.UNSUPPORTED;
		}

		if (osName.contains("Mac")) {
			if (osArch.equals("amd64")) return Platform.MACOS_64;
			return Platform.UNSUPPORTED;
		}

		return Platform.UNSUPPORTED;
	}
}
