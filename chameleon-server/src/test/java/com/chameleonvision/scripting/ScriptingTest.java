package com.chameleonvision.scripting;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.chameleonvision.scripting.ScriptManager.*;

public class ScriptingTest {

    @Test
    public void configTest() {
        ScriptConfigManager.deleteConfig();

        Assertions.assertFalse(ScriptConfigManager.fileExists());

        ScriptConfigManager.initialize();

        Assertions.assertTrue(ScriptConfigManager.fileExists());

        var config = ScriptConfigManager.loadConfig();
        Assertions.assertEquals(config.size(), ScriptEventType.values().length);
        System.out.println("Script Config PASSED");
    }

    @Test
    public void eventTest() {
        ScriptManager.queueEvent(ScriptEventType.kProgramInit);
    }
}
