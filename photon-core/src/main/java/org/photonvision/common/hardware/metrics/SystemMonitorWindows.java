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
     * OSHI uses jLibreHardwareMonitor to access the CPU Temperature on Windows. This is based on
     * LibreHardwareMonitor which includes Winring0, a kernel level driver. Windows defender flags
     * Winring0 as a severe vulnuratbility due to an unresolved CVE and blocks it from installing. In
     * the end, it isn't worth the risk to include this dependency, so no CPU temperature monitoring
     * on Windows.
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
