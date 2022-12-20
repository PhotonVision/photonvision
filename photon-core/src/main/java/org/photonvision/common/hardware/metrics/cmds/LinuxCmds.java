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

package org.photonvision.common.hardware.metrics.cmds;

import org.photonvision.common.configuration.HardwareConfig;

public class LinuxCmds extends CmdBase {
    public void initCmds(HardwareConfig config) {
        // CPU
        cpuMemoryCommand = "awk '/MemTotal:/ {print int($2 / 1000);}' /proc/meminfo";

        // TODO: boards have lots of thermal devices. Hard to pick the CPU

        cpuUtilizationCommand =
                "top -bn1 | grep \"Cpu(s)\" | sed \"s/.*, *\\([0-9.]*\\)%* id.*/\\1/\" | awk '{print 100 - $1}'";

        cpuUptimeCommand = "uptime -p | cut -c 4-";

        // RAM
        ramUsageCommand = "awk '/MemFree:/ {print int($2 / 1000);}' /proc/meminfo";

        // Disk
        diskUsageCommand = "df ./ --output=pcent | tail -n +2";
    }
}
