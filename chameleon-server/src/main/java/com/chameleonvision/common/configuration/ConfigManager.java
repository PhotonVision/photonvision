package com.chameleonvision.common.configuration;

public class ConfigManager {

	private final ConfigFolder rootFolder;

	protected ConfigManager() {

		rootFolder = new ConfigFolder("");
	}

	private static class SingletonHolder {
		private static final ConfigManager INSTANCE = new ConfigManager();
	}

	public static ConfigManager getInstance() {
		return SingletonHolder.INSTANCE;
	}
}
