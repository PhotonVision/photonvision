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

package org.photonvision.common.hardware.metrics;

public class CPU extends MetricsBase {

    private CPU() {}

    public static CPU getInstance() {
        return new CPU();
    }

    private static final String memoryCommand = "sudo vcgencmd get_mem arm | grep -Eo '[0-9]+'";
    private static final String temperatureCommand =
            "sudo cat /sys/class/thermal/thermal_zone0/temp | grep -x -E '[0-9]+'";
    private static final String utilizationCommand =
            "sudo top -bn1 | grep \"Cpu(s)\" | sed \"s/.*, *\\([0-9.]*\\)%* id.*/\\1/\" | awk '{print 100 - $1}'";

    public double getMemory() {
        return execute(memoryCommand);
    }

    public double getTemp() {
        return execute(temperatureCommand) / 1000;
    }

    public double getUtilization() {
        return execute(utilizationCommand);
    }
}
