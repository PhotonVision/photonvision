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

import java.util.HashMap;
import org.photonvision.common.util.ShellExec;

public abstract class PWMBase {
    public static HashMap<String, String> commands =
            new HashMap<>() {
                {
                    put("setRate", "");
                    put("setRange", "");
                    put("shutdown", "");
                }
            };

    private static final ShellExec runCommand = new ShellExec(true, true);

    public static String execute(String command) {
        try {
            runCommand.executeBashCommand(command);
        } catch (Exception e) {
            return "";
        }
        return runCommand.getOutput();
    }

    public abstract void setPwmRate(int rate);

    public abstract void setPwmRange(int[] range);

    public abstract int getPwmRate();

    public abstract int[] getPwmRange();

    public abstract boolean shutdown();
}
