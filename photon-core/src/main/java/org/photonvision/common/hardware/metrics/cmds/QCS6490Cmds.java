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

public class QCS6490Cmds extends LinuxCmds {
    /** Applies pi-specific commands, ignoring any input configuration */
    public void initCmds(HardwareConfig config) {
        super.initCmds(config);

        /**
         * for zone in /sys/class/thermal/thermal_zone*; do echo "=== $(basename $zone) ===" echo "Type:
         * $(cat $zone/type 2>/dev/null || echo 'N/A')" echo "Temp: $(cat $zone/temp 2>/dev/null || echo
         * 'N/A')" echo "Policy: $(cat $zone/policy 2>/dev/null || echo 'N/A')" echo done
         */
        // The command below gets the temperaure of the CPU thermal zone. To find a different thermal
        // zone, run the above command on a Rubik Pi 3.

        cpuTemperatureCommand =
                "cat /sys/class/thermal/thermal_zone10/temp | awk '{printf \"%.1f\", $1/1000}'";
        // TODO: NPU usage, doesn't seem to be in the same place as the opi. We're gonna just wait on QC
        // to get back to us on this one.
    }
}
