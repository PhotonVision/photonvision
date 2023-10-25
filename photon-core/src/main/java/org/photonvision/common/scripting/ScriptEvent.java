/*
 * Copyright (C) Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.photonvision.common.scripting;

import java.io.IOException;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.ShellExec;

public class ScriptEvent {
    private static final ShellExec executor = new ShellExec(true, true);

    public final ScriptConfig config;
    private final Logger logger = new Logger(ScriptEvent.class, LogGroup.General);

    public ScriptEvent(ScriptConfig config) {
        this.config = config;
    }

    public int run() throws IOException {
        int retVal = executor.executeBashCommand(config.command);

        String output = executor.getOutput();
        String error = executor.getError();

        if (!error.isEmpty()) {
            logger.error("Error when running \"" + config.eventType.name() + "\" script: " + error);
        } else if (!output.isEmpty()) {
            logger.info(
                    String.format("Output from \"%s\" script: %s\n", config.eventType.name(), output));
        }
        logger.info(
                String.format(
                        "Script for %s ran with command line: \"%s\", exit code: %d, output: %s, "
                                + "error: %s\n",
                        config.eventType.name(), config.command, retVal, output, error));
        return retVal;
    }
}
