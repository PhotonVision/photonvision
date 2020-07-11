/*
 * Copyright (C) 2020 Photon Vision.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("unused")
public class HardwareConfig {

    public final String deviceName;
    public final String deviceLogoPath;
    public final String supportURL;

    // LED control
    public final int[] ledPins;
    public final String ledSetCommand;
    public final boolean ledsCanDim;
    public final int[] ledPWMRange;
    public final String ledDimCommand;

    // Metrics
    public final String cpuTempCommand;
    public final String cpuMemoryCommand;
    public final String cpuUtilCommand;
    public final String gpuMemoryCommand;
    public final String gpuUtilCommand;
    public final String ramUtilCommand;

    public HardwareConfig() {
        deviceName = "";
        deviceLogoPath = "";
        supportURL = "";
        ledPins = new int[0];
        ledSetCommand = "";
        ledsCanDim = false;
        ledPWMRange = new int[0];
        ledDimCommand = "";

        cpuTempCommand = "";
        cpuMemoryCommand = "";
        cpuUtilCommand = "";
        gpuMemoryCommand = "";
        gpuUtilCommand = "";
        ramUtilCommand = "";
    }

    @JsonCreator
    public HardwareConfig(
            @JsonProperty("deviceName") String deviceName,
            @JsonProperty("deviceLogoPath") String deviceLogoPath,
            @JsonProperty("supportURL") String supportURL,
            @JsonProperty("ledPins") int[] ledPins,
            @JsonProperty("ledSetCommand") String ledSetCommand,
            @JsonProperty("ledsCanDim") boolean ledsCanDim,
            @JsonProperty("ledPWMRange") int[] ledPWMRange,
            @JsonProperty("ledDimCommand") String ledDimCommand,
            @JsonProperty("cpuTempCommand") String cpuTempCommand,
            @JsonProperty("cpuMemoryCommand") String cpuMemoryCommand,
            @JsonProperty("cpuUtilCommand") String cpuUtilCommand,
            @JsonProperty("gpuMemoryCommand") String gpuMemoryCommand,
            @JsonProperty("gpuUtilCommand") String gpuUtilCommand,
            @JsonProperty("ramUtilCommand") String ramUtilCommand
            ) {
        this.deviceName = deviceName;
        this.deviceLogoPath = deviceLogoPath;
        this.supportURL = supportURL;

        this.ledPins = ledPins;
        this.ledSetCommand = ledSetCommand;
        this.ledsCanDim = ledsCanDim;
        this.ledPWMRange = ledPWMRange;
        this.ledDimCommand = ledDimCommand;

        this.cpuTempCommand = cpuTempCommand;
        this.cpuMemoryCommand = cpuMemoryCommand;
        this.cpuUtilCommand = cpuUtilCommand;
        this.gpuMemoryCommand = gpuMemoryCommand;
        this.gpuUtilCommand = gpuUtilCommand;
        this.ramUtilCommand = ramUtilCommand;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getDeviceLogoPath() {
        return deviceLogoPath;
    }

    public String getSupportURL() {
        return supportURL;
    }

    public int[] getLedPins() {
        return ledPins;
    }

    public String getLedSetCommand() {
        return ledSetCommand;
    }

    public boolean isLedsCanDim() {
        return ledsCanDim;
    }

    public int[] getLedPWMRange() {
        return ledPWMRange;
    }

    public String getLedDimCommand() {
        return ledDimCommand;
    }

    public String getCpuTempCommand() {
        return cpuTempCommand;
    }

    public String getCpuMemoryCommand() {
        return cpuMemoryCommand;
    }

    public String getCpuUtilCommand() {
        return cpuUtilCommand;
    }

    public String getGpuMemoryCommand() {
        return gpuMemoryCommand;
    }

    public String getGpuUtilCommand() {
        return gpuUtilCommand;
    }

    public String getRamUtilCommand() {
        return ramUtilCommand;
    }
}
