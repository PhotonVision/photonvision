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

        // TODO: temperature. We can get thermal zones, we just gotta figure out what's what.
        // TODO: NPU usage, we might be able to get it from the debug section in /sys/ but we're waiting
        // on the ubuntu image for that.
    }
}
