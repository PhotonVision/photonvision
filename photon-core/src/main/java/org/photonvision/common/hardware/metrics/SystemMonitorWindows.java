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
     * data to WMI. As a work-around, OSHI tries to use LibreHardwareMonitor via
     * jLibreHardwareMonitor. If the temperature isn't found in WMI and jLibreHardwareMonitor isn't
     * present, OSHI issues warnings every time getCpuTemperature() is called. This clogs the console
     * with useless information when running on Windows and makes testing difficult.
     *
     * <p>We could include jLibreHardwareMonitor as a dependency for our Windows jar, but
     * LibreHardwareMonitor installs Winring0.sys, which is a kernel-level driver with an unfixed
     * severe vulnerability. Windows defender flags Winring0 as a vulnerable driver and blocks it from
     * installing.
     *
     * <p>In the end, it isn't worth the risk to include this dependency, so we don't do CPU
     * temperature monitoring on Windows.
     *
     * <p>Threat Information:
     * https://www.microsoft.com/en-us/wdsi/threats/threat-search?query=VulnerableDriver:WinNT/Winring0
     * Understanding Winring0 vulnerability alert:
     * https://windowsforum.com/threads/understanding-microsoft-defenders-vulnerabledriver-winring0-alert-and-how-to-respond.373544/
     */
    @Override
    public double getCpuTemperature() {
        return -1.0;
    }
}
