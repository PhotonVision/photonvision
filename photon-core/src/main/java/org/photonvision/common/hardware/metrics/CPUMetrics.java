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

public class CPUMetrics extends MetricsBase {

    private String cpuMemSplit = null;

    public String getMemory() {
        if (cpuMemoryCommand.isEmpty()) return "";
        if (cpuMemSplit == null) {
            cpuMemSplit = execute(cpuMemoryCommand);
        }
        return cpuMemSplit;
    }

    public String getTemp() {
        if (cpuTemperatureCommand.isEmpty()) return "";
        try {
            return execute(cpuTemperatureCommand);
        } catch (Exception e) {
            return "N/A";
        }
    }

    public String getUtilization() {
        return execute(cpuUtilizationCommand);
    }
}
