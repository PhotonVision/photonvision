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

public class PiCmds extends CmdBase {
    /** Applies pi-specific commands, ignoring any input configuration */
    public void initCmds(HardwareConfig config) {
        // CPU
        cpuMemoryCommand = "vcgencmd get_mem arm | grep -Eo '[0-9]+'";
        cpuTemperatureCommand = "sed 's/.\\{3\\}$/.&/' <<< cat /sys/class/thermal/thermal_zone0/temp";
        cpuUtilizationCommand =
                "top -bn1 | grep \"Cpu(s)\" | sed \"s/.*, *\\([0-9.]*\\)%* id.*/\\1/\" | awk '{print 100 - $1}'";

        cpuThrottleReasonCmd =
                "if   ((  $(( $(vcgencmd get_throttled | grep -Eo 0x[0-9a-fA-F]*) & 0x01 )) != 0x00 )); then echo \"LOW VOLTAGE\"; "
                        + " elif ((  $(( $(vcgencmd get_throttled | grep -Eo 0x[0-9a-fA-F]*) & 0x08 )) != 0x00 )); then echo \"HIGH TEMP\"; "
                        + " elif ((  $(( $(vcgencmd get_throttled | grep -Eo 0x[0-9a-fA-F]*) & 0x10000 )) != 0x00 )); then echo \"Prev. Low Voltage\"; "
                        + " elif ((  $(( $(vcgencmd get_throttled | grep -Eo 0x[0-9a-fA-F]*) & 0x80000 )) != 0x00 )); then echo \"Prev. High Temp\"; "
                        + " else echo \"None\"; fi";

        cpuUptimeCommand = "uptime -p | cut -c 4-";

        // GPU
        gpuMemoryCommand = "vcgencmd get_mem gpu | grep -Eo '[0-9]+'";
        gpuMemUsageCommand = "vcgencmd get_mem malloc | grep -Eo '[0-9]+'";

        // RAM
        ramUsageCommand = "free --mega | awk -v i=2 -v j=3 'FNR == i {print $j}'";

        // Disk
        diskUsageCommand = "df ./ --output=pcent | tail -n +2";
    }
}
