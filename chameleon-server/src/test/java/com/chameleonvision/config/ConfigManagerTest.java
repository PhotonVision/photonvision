package com.chameleonvision.config;

import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.nio.file.Files;

public class ConfigManagerTest {

    @BeforeAll
    public void deleteConfig() {
        try {
            Files.delete(ConfigManager.SettingsPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
