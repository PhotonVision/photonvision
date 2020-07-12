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

    private final String deviceName;
    private final String deviceLogoPath;
    private final String supportURL;

    // LED control
    private final int[] ledPins;
    private final String ledSetCommand;
    private final boolean ledsCanDim;
    private final int[] ledPWMRange;
    private final String ledPWMSetRange;
    private final String ledPWMSetRate;
    private final String ledDimCommand;
    private final String ledBlinkCommand;
    private final String ledPulseCommand;

    // Metrics
    private final String cpuTempCommand;
    private final String cpuMemoryCommand;
    private final String cpuUtilCommand;
    private final String gpuMemoryCommand;
    private final String gpuTempCommand;
    private final String ramUtilCommand;

    public HardwareConfig() {
        deviceName = "";
        deviceLogoPath = "";
        supportURL = "";
        ledPins = new int[0];
        ledSetCommand = "";
        ledsCanDim = false;
        ledPWMRange = new int[0];
        ledPWMSetRange = "";
        ledPWMSetRate = "";
        ledDimCommand = "";

        cpuTempCommand = "";
        cpuMemoryCommand = "";
        cpuUtilCommand = "";
        gpuMemoryCommand = "";
        gpuTempCommand = "";
        ramUtilCommand = "";
        ledBlinkCommand = "";
        ledPulseCommand = "";
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
            @JsonProperty("ledPWMSetRange") String ledPWMSetRange,
            @JsonProperty("ledPWMSetRate") String ledPWMSetRate,
            @JsonProperty("ledDimCommand") String ledDimCommand,
            @JsonProperty("ledBlinkCommand") String ledBlinkCommand,
            @JsonProperty("ledPulseCommand") String ledPulseCommand,
            @JsonProperty("cpuTempCommand") String cpuTempCommand,
            @JsonProperty("cpuMemoryCommand") String cpuMemoryCommand,
            @JsonProperty("cpuUtilCommand") String cpuUtilCommand,
            @JsonProperty("gpuMemoryCommand") String gpuMemoryCommand,
            @JsonProperty("gpuTempCommand") String gpuTempCommand,
            @JsonProperty("ramUtilCommand") String ramUtilCommand) {
        this.deviceName = deviceName;
        this.deviceLogoPath = deviceLogoPath;
        this.supportURL = supportURL;

        this.ledPins = ledPins;
        this.ledSetCommand = ledSetCommand;
        this.ledsCanDim = ledsCanDim;
        this.ledPWMRange = ledPWMRange;
        this.ledPWMSetRange = ledPWMSetRange;
        this.ledPWMSetRate = ledPWMSetRate;
        this.ledDimCommand = ledDimCommand;
        this.ledBlinkCommand = ledBlinkCommand;
        this.ledPulseCommand = ledPulseCommand;

        this.cpuTempCommand = cpuTempCommand;
        this.cpuMemoryCommand = cpuMemoryCommand;
        this.cpuUtilCommand = cpuUtilCommand;
        this.gpuMemoryCommand = gpuMemoryCommand;
        this.gpuTempCommand = gpuTempCommand;
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

    public String getLedBlinkCommand() {
        return ledBlinkCommand;
    }

    public String getLedPulseCommand() {
        return ledPulseCommand;
    }

    public boolean isLedsCanDim() {
        return ledsCanDim;
    }

    public int[] getLedPWMRange() {
        return ledPWMRange;
    }

    public String getLedPWMSetRange() {
        return ledPWMSetRange;
    }

    public String getLedPWMSetRate() {
        return ledPWMSetRate;
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

    public String getGpuTempCommand() {
        return gpuTempCommand;
    }

    public String getRamUtilCommand() {
        return ramUtilCommand;
    }
}
