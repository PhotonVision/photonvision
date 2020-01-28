package com.chameleonvision.scripting;

import com.chameleonvision.Debug;
import com.chameleonvision.util.ShellExec;

import java.io.IOException;

public class ScriptEvent {
    private static final ShellExec executor = new ShellExec(true, true);

    public final ScriptConfig config;

    public ScriptEvent(ScriptConfig config) {
        this.config = config;
    }

    public int run() throws IOException {
        int retVal = executor.executeBashCommand(config.command);

        String output = executor.getOutput();
        String error = executor.getError();

        if (!error.isEmpty()) {
            System.err.printf("Error when running \"%s\" script: %s\n", config.eventType.name(), error);
        } else if (!output.isEmpty()) {
            Debug.printInfo(String.format("Output from \"%s\" script: %s\n", config.eventType.name(), output));
        }
        Debug.printInfo(String.format("Script for %s ran with command line: \"%s\", exit code: %d, output: %s, error: %s\n", config.eventType.name(), config.command, retVal, output, error));
        return retVal;
    }
}
