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

public class RK3588Cmds extends LinuxCmds {
    /** Applies pi-specific commands, ignoring any input configuration */
    public void initCmds(HardwareConfig config) {
        super.initCmds(config);

        // CPU Temperature
        /* The RK3588 chip has 7 thermal zones that can be accessed via:
         *       /sys/class/thermal/thermal_zoneX/temp
         * where X is an integer from 0 to 6.
         *
         * || Zone || Location    || Comments                                                ||
         * |    0  |  soc         |  soc thermal (near the center of the chip)                |
         * |    1  |  bigcore0    |  CPU Big Core A76_0/1 (CPU4 and CPU5)                     |
         * |    2  |  bigcore1    |  CPU Big Core A76_2/3 (CPU6 and CPU7)                     |
         * |    3  |  littlecore  |  CPU Small Core A55_0/1/2/3 (CPU0, CPU1, CPU2, and CPU3)  |
         * |    4  |  center      |  also called PD_CENTER                                    |
         * |    5  |  gpu         |  GPU                                                      |
         * |    6  |  npu         |  NPU                                                      |
         *
         * Sources:
         *   - http://forum.armsom.org/t/topic/51/3
         *   - https://lore.kernel.org/lkml/7276280.TLKafQO6qx@archbook/
         */
        cpuTemperatureCommand =
                "cat /sys/class/thermal/thermal_zone1/temp | awk '{printf \"%.1f\", $1/1000}'";

        npuUsageCommand = "cat /sys/kernel/debug/rknpu/load | sed 's/NPU load://; s/^ *//; s/ *$//'";
    }
}
