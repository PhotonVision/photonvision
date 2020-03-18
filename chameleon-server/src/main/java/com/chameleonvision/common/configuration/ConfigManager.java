package com.chameleonvision.common.configuration;

import com.chameleonvision.common.server.configuration.MainConfig;

public class ConfigManager {

    private final ConfigFolder rootFolder;
    final MainConfig mainConfig;

    protected ConfigManager() {

        rootFolder = new ConfigFolder("");

        mainConfig = MainConfig.getInstance();
    }

    private static class SingletonHolder {
        private static final ConfigManager INSTANCE = new ConfigManager();
    }

    public static ConfigManager getInstance() {
        return SingletonHolder.INSTANCE;
    }
}
