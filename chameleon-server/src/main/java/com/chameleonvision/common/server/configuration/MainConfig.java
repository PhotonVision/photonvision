package com.chameleonvision.common.server.configuration;

import com.chameleonvision.common.configuration.ConfigFile;

public class MainConfig extends ConfigFile {

    public int teamNumber = 0;
    public boolean ntServer = false;

    private MainConfig() {
        super("general");
    }

    private static class SingletonHolder {
        private static final MainConfig INSTANCE = new MainConfig();
    }

    public static MainConfig getInstance() {
        return SingletonHolder.INSTANCE;
    }
}
