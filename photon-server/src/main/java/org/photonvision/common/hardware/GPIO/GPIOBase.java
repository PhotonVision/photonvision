/*
 * Copyright (C) 2020 Photon Vision.
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

package org.photonvision.common.hardware.GPIO;

import java.util.Arrays;
import java.util.HashMap;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.ShellExec;

public abstract class GPIOBase {
    private static final Logger logger = new Logger(GPIOBase.class, LogGroup.General);

    public static HashMap<String, String> commands =
            new HashMap<>() {
                {
                    put("setState", "");
                    put("blink", "");
                    put("shutdown", "");
                }
            };

    private static final ShellExec runCommand = new ShellExec(true, true);

    public static String execute(String command) {
        try {
            runCommand.executeBashCommand(command);
        } catch (Exception e) {
            logger.error(Arrays.toString(e.getStackTrace()));
            return "";
        }
        return runCommand.getOutput();
    }

    public abstract void togglePin();

    public abstract void setLow();

    public abstract void setHigh();

    public abstract void setState(boolean state);

    public abstract void blink(long delay, long duration);

    public abstract boolean shutdown();

    public abstract boolean getState();
}
