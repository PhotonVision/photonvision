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

package org.photonvision.common.hardware.metrics;

import java.io.IOException;
import org.photonvision.common.util.ShellExec;

public class SystemMonitorRaspberryPi extends SystemMonitor {
    private final ShellExec runCommand = new ShellExec(true, true);

    @Override
    public String getCpuThrottleReason() {
        int state = 0;
        String output = vcgencmd("get_throttled");
        try {
            state = Integer.decode(output);
        } catch (NumberFormatException e) {
            logger.warn("Could not parse return value: " + output);
        }
        if ((state & 0x01) != 0) {
            return "LOW VOLTAGE";
        } else if ((state & 0x08) != 0) {
            return "HIGH TEMP";
        } else if ((state & 0x10000) != 0) {
            return "Prev. Low Voltage";
        } else if ((state & 0x80000) != 0) {
            return "Prev. High Temp";
        }
        return "None";
    }

    @Override
    public double getGpuMem() {
        String output = vcgencmd("get_mem gpu");
        if (!output.isBlank()) {
            return Integer.parseInt(output);
        }
        return -1.0;
    }

    @Override
    public double getGpuMemUtil() {
        String output = vcgencmd("get_mem malloc");
        if (!output.isBlank()) {
            return Integer.parseInt(output);
        }
        return -1.0;
    }

    private String vcgencmd(String cmd) {
        if (cmd.isBlank()) {
            return "";
        }
        String command = "vcgencmd " + cmd;
        try {
            runCommand.executeBashCommand(command, true, false);
            if (runCommand.getExitCode() != 0) {
                logger.error("Bad response from vcgencmd: " + runCommand.getOutput());
                return "";
            } else {
                return runCommand.getOutput().split("=")[1].replaceAll("[^\\d.]$", "");
            }
        } catch (IOException e) {
            logger.error("Could not run `vcgencmd`!", e);
            return "";
        }
    }
}
