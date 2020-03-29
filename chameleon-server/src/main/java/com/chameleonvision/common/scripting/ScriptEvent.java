package com.chameleonvision.common.scripting;

import com.chameleonvision.common.logging.DebugLogger;
import com.chameleonvision.common.util.ShellExec;
import java.io.IOException;

public class ScriptEvent {
    private static final DebugLogger logger = new DebugLogger(true);
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
            logger.printInfo(
                    String.format("Output from \"%s\" script: %s\n", config.eventType.name(), output));
        }
        logger.printInfo(
                String.format(
                        "Script for %s ran with command line: \"%s\", exit code: %d, output: %s, error: %s\n",
                        config.eventType.name(), config.command, retVal, output, error));
        return retVal;
    }
}
