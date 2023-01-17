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

package org.photonvision.common.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HardwareConfig {
    public final String deviceName;
    public final String deviceLogoPath;
    public final String supportURL;

    // LED control
    public final ArrayList<Integer> ledPins;
    public final String ledSetCommand;
    public final boolean ledsCanDim;
    public final ArrayList<Integer> ledBrightnessRange;
    public final String ledDimCommand;
    public final String ledBlinkCommand;
    public final ArrayList<Integer> statusRGBPins;

    // Metrics
    public final String cpuTempCommand;
    public final String cpuMemoryCommand;
    public final String cpuUtilCommand;
    public final String cpuThrottleReasonCmd;
    public final String cpuUptimeCommand;
    public final String gpuMemoryCommand;
    public final String ramUtilCommand;
    public final String gpuMemUsageCommand;
    public final String diskUsageCommand;

    // Device stuff
    public final String restartHardwareCommand;
    public final double vendorFOV; // -1 for unmanaged
    public final List<Integer> blacklistedResIndices; // this happens before the defaults are applied

    public HardwareConfig() {
        deviceName = "";
        deviceLogoPath = "";
        supportURL = "";
        ledPins = new ArrayList<>();
        ledSetCommand = "";
        ledsCanDim = false;
        ledBrightnessRange = new ArrayList<>();
        statusRGBPins = new ArrayList<>();
        ledDimCommand = "";

        cpuTempCommand = "";
        cpuMemoryCommand = "";
        cpuUtilCommand = "";
        cpuThrottleReasonCmd = "";
        cpuUptimeCommand = "";
        gpuMemoryCommand = "";
        ramUtilCommand = "";
        ledBlinkCommand = "";
        gpuMemUsageCommand = "";
        diskUsageCommand = "";

        restartHardwareCommand = "";
        vendorFOV = -1;
        blacklistedResIndices = Collections.emptyList();
    }

    @SuppressWarnings("unused")
    public HardwareConfig(
            String deviceName,
            String deviceLogoPath,
            String supportURL,
            ArrayList<Integer> ledPins,
            String ledSetCommand,
            boolean ledsCanDim,
            ArrayList<Integer> ledBrightnessRange,
            String ledDimCommand,
            String ledBlinkCommand,
            ArrayList<Integer> statusRGBPins,
            String cpuTempCommand,
            String cpuMemoryCommand,
            String cpuUtilCommand,
            String cpuThrottleReasonCmd,
            String cpuUptimeCommand,
            String gpuMemoryCommand,
            String ramUtilCommand,
            String gpuMemUsageCommand,
            String diskUsageCommand,
            String restartHardwareCommand,
            double vendorFOV,
            List<Integer> blacklistedResIndices) {
        this.deviceName = deviceName;
        this.deviceLogoPath = deviceLogoPath;
        this.supportURL = supportURL;
        this.ledPins = ledPins;
        this.ledSetCommand = ledSetCommand;
        this.ledsCanDim = ledsCanDim;
        this.ledBrightnessRange = ledBrightnessRange;
        this.ledDimCommand = ledDimCommand;
        this.ledBlinkCommand = ledBlinkCommand;
        this.statusRGBPins = statusRGBPins;
        this.cpuTempCommand = cpuTempCommand;
        this.cpuMemoryCommand = cpuMemoryCommand;
        this.cpuUtilCommand = cpuUtilCommand;
        this.cpuThrottleReasonCmd = cpuThrottleReasonCmd;
        this.cpuUptimeCommand = cpuUptimeCommand;
        this.gpuMemoryCommand = gpuMemoryCommand;
        this.ramUtilCommand = ramUtilCommand;
        this.gpuMemUsageCommand = gpuMemUsageCommand;
        this.diskUsageCommand = diskUsageCommand;
        this.restartHardwareCommand = restartHardwareCommand;
        this.vendorFOV = vendorFOV;
        this.blacklistedResIndices = blacklistedResIndices;
    }

    /** @return True if the FOV has been preset to a sane value, false otherwise */
    public final boolean hasPresetFOV() {
        return vendorFOV > 0;
    }

    /** @return True if any command has been configured to a non-default empty, false otherwise */
    public final boolean hasCommandsConfigured() {
        return cpuTempCommand != ""
                || cpuMemoryCommand != ""
                || cpuUtilCommand != ""
                || cpuThrottleReasonCmd != ""
                || cpuUptimeCommand != ""
                || gpuMemoryCommand != ""
                || ramUtilCommand != ""
                || ledBlinkCommand != ""
                || gpuMemUsageCommand != ""
                || diskUsageCommand != "";
    }
}
