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

package org.photonvision.common.hardware.PWM;

import eu.xeli.jpigpio.JPigpio;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.ShellExec;

public abstract class PWMBase {
    private static final Logger logger = new Logger(PWMBase.class, LogGroup.General);
    public static JPigpio pigpio;

    public static HashMap<String, String> commands =
            new HashMap<>() {
                {
                    put("setRange", "");
                    put("shutdown", "");
                    put("dim", "");
                    put("blink", "");
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

    public abstract void setPwmRange(List<Integer> range);

    public abstract List<Integer> getPwmRange();

    public abstract void blink(int pulseTimeMillis, int  blinks);

    public abstract boolean shutdown();

    public abstract void dimLED(int dimPercentage);
}
