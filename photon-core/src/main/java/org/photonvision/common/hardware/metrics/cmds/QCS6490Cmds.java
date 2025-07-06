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

        /* Thermal zone information can be found in /sys/class/thermal/thermal_zone* directories:
         * zone/type: Contains the thermal zone type/name (e.g., "acpi", "x86_pkg_temp")
         * zone/temp: Current temperature in millidegrees Celsius (divide by 1000 for actual temp)
         * zone/policy: Thermal governor policy (e.g., "step_wise", "power_allocator")
         * Each thermal_zone* directory represents a different temperature sensor in the system
         */

        cpuTemperatureCommand =
                "cat /sys/class/thermal/thermal_zone10/temp | awk '{printf \"%.1f\", $1/1000}'";

        // TODO: NPU usage, doesn't seem to be in the same place as the opi. We're gonna just wait on QC
        // to get back to us on this one.
    }
}
