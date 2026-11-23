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

public class SystemMonitorWindows extends SystemMonitor {
    /**
     * Monitoring CPU Temperature on Windows is challenging because most vendors don't publish this
     * data to WMI. As a work-around, OSHI tries to use LibreHardwareMonitor. Since OSHI version
     * 7.8.0, OSHI has removed the LibreHardwareMonitor dependency for Windows, so we can't use it to
     * get CPU temperature. Instead, return -1.0 to indicate that the temperature is unavailable.
     *
     * <p>For more information, see <a href="https://github.com/oshi/oshi/issues/3905">OSHI Issue
     * #3905</a>
     */
    @Override
    public double getCpuTemperature() {
        return -1.0;
    }
}
