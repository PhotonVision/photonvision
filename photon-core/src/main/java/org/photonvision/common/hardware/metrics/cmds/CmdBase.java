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

public class CmdBase {
    // CPU
    public String cpuMemoryCommand = "";
    public String cpuTemperatureCommand = "";
    public String cpuUtilizationCommand = "";
    public String cpuThrottleReasonCmd = "";
    public String cpuUptimeCommand = "";
    // GPU
    public String gpuMemoryCommand = "";
    public String gpuMemUsageCommand = "";
    // RAM
    public String ramUsageCommand = "";
    // Disk
    public String diskUsageCommand = "";

    public void initCmds(HardwareConfig config) {
        return; // default - do nothing
    }
}
