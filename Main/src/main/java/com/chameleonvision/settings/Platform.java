package com.chameleonvision.settings;

import com.chameleonvision.util.ShellExec;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public enum Platform {
	WINDOWS_64("Windows x64"),
	LINUX_64("Linux x64"),
	LINUX_RASPBIAN("Linux Raspbian"),
	LINUX_ARM64("Linux ARM64"),
	MACOS_64("Mac OS x64"),
	UNSUPPORTED("Unsupported Platform");

	public final String value;

	Platform(String value) {
		this.value = value;
	}

	private static final String OS_NAME =  System.getProperty("os.name");
	private static final String OS_ARCH = System.getProperty("os.arch");

	public boolean isWindows() {
		return this == WINDOWS_64;
	}

	public boolean isLinux() {
		return this == LINUX_64 || this == LINUX_RASPBIAN || this == LINUX_ARM64;
	}

	public boolean isMac() {
		return this == MACOS_64;
	}

	private static ShellExec shell = new ShellExec(true, false);

	public boolean isRoot() {
		if (isLinux() || isMac()) {
			try {
				shell.execute("id", null, true, "-u");
			} catch (IOException e) {
				e.printStackTrace();
			}
			while (!shell.isOutputCompleted()) {}
			if (shell.getExitCode() == 0) {
				var out = shell.getOutput();
				out = out.split("\n")[0];
				return out.equals("0");
			}
		} else if (isWindows()) {
			return true;
		} else {
			return true;
		}
		return false;
	}

	private static boolean isRaspbian() {
		try (BufferedReader reader = Files.newBufferedReader(Paths.get("/etc/os-release"))) {
			String value = reader.readLine();
			return value.contains("Raspbian");
		} catch (IOException ex) {
			return false;
		}
	}

	public static Platform getCurrentPlatform() {
		if (OS_NAME.contains("Windows")) {
			if (OS_ARCH.equals("amd64")) return Platform.WINDOWS_64;
		}

		if (OS_NAME.contains("Linux")) {
			if (OS_ARCH.equals("amd64")) return Platform.LINUX_64;
			if (isRaspbian()) return Platform.LINUX_RASPBIAN;
			if (OS_ARCH.contains("aarch")) return Platform.LINUX_ARM64;
		}

		if (OS_NAME.contains("Mac")) {
			if (OS_ARCH.equals("amd64")) return Platform.MACOS_64;
		}

		System.out.printf("Unknown Platform! OS: %s, Architecture: %s", OS_NAME, OS_ARCH);
		return Platform.UNSUPPORTED;
	}

	public String toString() {
		if (this.equals(UNSUPPORTED)) {
			return String.format("Unknown Platform. OS: %s, Architecture: %s", OS_NAME, OS_ARCH);
		} else {
			return this.value;
		}
	}
}
