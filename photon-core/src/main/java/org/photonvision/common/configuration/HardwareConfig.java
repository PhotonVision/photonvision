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

@JsonIgnoreProperties(ignoreUnknown = true)
public class HardwareConfig {
    public final String deviceName;
    public final String deviceLogoPath;
    public final String supportURL;

    // LED control
    public final ArrayList<Integer> ledPins;
    public final boolean ledsCanDim;
    public final ArrayList<Integer> ledBrightnessRange;
    public final ArrayList<Integer> statusRGBPins;

    // Custom GPIO
    public final String getGPIOCommand;
    public final String setGPIOCommand;
    public final String setPWMCommand;
    public final String releaseGPIOCommand;

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

    public HardwareConfig(
            String deviceName,
            String deviceLogoPath,
            String supportURL,
            ArrayList<Integer> ledPins,
            boolean ledsCanDim,
            ArrayList<Integer> ledBrightnessRange,
            ArrayList<Integer> statusRGBPins,
            String getGPIOCommand,
            String setGPIOCommand,
            String setPWMCommand,
            String releaseGPIOCommand,
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
            double vendorFOV) {
        this.deviceName = deviceName;
        this.deviceLogoPath = deviceLogoPath;
        this.supportURL = supportURL;
        this.ledPins = ledPins;
        this.ledsCanDim = ledsCanDim;
        this.ledBrightnessRange = ledBrightnessRange;
        this.statusRGBPins = statusRGBPins;
        this.getGPIOCommand = getGPIOCommand;
        this.setGPIOCommand = setGPIOCommand;
        this.setPWMCommand = setPWMCommand;
        this.releaseGPIOCommand = releaseGPIOCommand;
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
    }

    public HardwareConfig() {
        deviceName = "";
        deviceLogoPath = "";
        supportURL = "";
        ledPins = new ArrayList<>();
        ledsCanDim = false;
        ledBrightnessRange = new ArrayList<>();
        statusRGBPins = new ArrayList<>();
        getGPIOCommand = "";
        setGPIOCommand = "";
        setPWMCommand = "";
        releaseGPIOCommand = "";
        cpuTempCommand = "";
        cpuMemoryCommand = "";
        cpuUtilCommand = "";
        cpuThrottleReasonCmd = "";
        cpuUptimeCommand = "";
        gpuMemoryCommand = "";
        ramUtilCommand = "";
        gpuMemUsageCommand = "";
        diskUsageCommand = "";
        restartHardwareCommand = "";
        vendorFOV = -1;
    }

    /**
     * @return True if the FOV has been preset to a sane value, false otherwise
     */
    public final boolean hasPresetFOV() {
        return vendorFOV > 0;
    }

    /**
     * @return True if any info command has been configured to be non-empty, false otherwise
     */
    public final boolean hasCommandsConfigured() {
        return cpuTempCommand != ""
                || cpuMemoryCommand != ""
                || cpuUtilCommand != ""
                || cpuThrottleReasonCmd != ""
                || cpuUptimeCommand != ""
                || gpuMemoryCommand != ""
                || ramUtilCommand != ""
                || gpuMemUsageCommand != ""
                || diskUsageCommand != "";
    }

    /**
     * @return True if any gpio command has been configured to be non-empty, false otherwise
     */
    public final boolean hasGPIOCommandsConfigured() {
        return getGPIOCommand != ""
                || setGPIOCommand != ""
                || setPWMCommand != ""
                || releaseGPIOCommand != "";
    }

    @Override
    public String toString() {
        return "HardwareConfig[deviceName="
                + deviceName
                + ", deviceLogoPath="
                + deviceLogoPath
                + ", supportURL="
                + supportURL
                + ", ledPins="
                + ledPins
                + ", ledsCanDim="
                + ledsCanDim
                + ", ledBrightnessRange="
                + ledBrightnessRange
                + ", statusRGBPins="
                + statusRGBPins
                + ", getGPIOCommand="
                + getGPIOCommand
                + ", setGPIOCommand="
                + setGPIOCommand
                + ", setPWMCommand="
                + setPWMCommand
                + ", releaseGPIOCommand="
                + releaseGPIOCommand
                + ", cpuTempCommand="
                + cpuTempCommand
                + ", cpuMemoryCommand="
                + cpuMemoryCommand
                + ", cpuUtilCommand="
                + cpuUtilCommand
                + ", cpuThrottleReasonCmd="
                + cpuThrottleReasonCmd
                + ", cpuUptimeCommand="
                + cpuUptimeCommand
                + ", gpuMemoryCommand="
                + gpuMemoryCommand
                + ", ramUtilCommand="
                + ramUtilCommand
                + ", gpuMemUsageCommand="
                + gpuMemUsageCommand
                + ", diskUsageCommand="
                + diskUsageCommand
                + ", restartHardwareCommand="
                + restartHardwareCommand
                + ", vendorFOV="
                + vendorFOV
                + "]";
    }
}
