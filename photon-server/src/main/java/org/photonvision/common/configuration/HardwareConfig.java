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
import java.util.ArrayList;
import java.util.Map;

@SuppressWarnings("unused")
public class HardwareConfig {

    private final String deviceName;
    private final String deviceLogoPath;
    private final String supportURL;

    // LED control
    private final ArrayList<Integer> ledPins;
    private final String ledSetCommand;
    private final boolean ledsCanDim;
    private final ArrayList<Integer> ledPWMRange;
    private final String ledPWMSetRange;
    private final String ledDimCommand;
    private final String ledBlinkCommand;

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
        ledPins = new ArrayList<>();
        ledSetCommand = "";
        ledsCanDim = false;
        ledPWMRange = new ArrayList<>();
        ledPWMSetRange = "";
        ledDimCommand = "";

        cpuTempCommand = "";
        cpuMemoryCommand = "";
        cpuUtilCommand = "";
        gpuMemoryCommand = "";
        gpuTempCommand = "";
        ramUtilCommand = "";
        ledBlinkCommand = "";
    }

    @JsonCreator
    public HardwareConfig(
            @JsonProperty("deviceName") String deviceName,
            @JsonProperty("deviceLogoPath") String deviceLogoPath,
            @JsonProperty("supportURL") String supportURL,
            @JsonProperty("hardware") Map<String, ?> hardware,
            @JsonProperty("metrics") Map<String, ?> metrics) {
        this.deviceName = deviceName;
        this.deviceLogoPath = deviceLogoPath;
        this.supportURL = supportURL;
        this.ledPins = (ArrayList<Integer>) hardware.get("leds");
        this.ledSetCommand = (String) hardware.get("ledSetCommand");
        this.ledsCanDim = (Boolean) hardware.get("ledsCanDim");
        this.ledPWMRange = (ArrayList<Integer>) hardware.get("ledPWMRange");
        this.ledPWMSetRange = (String) hardware.get("ledPWMSetRange");
        this.ledDimCommand = (String) hardware.get("ledDimCommand");
        this.ledBlinkCommand = (String) hardware.get("ledBlinkCommand");

        this.cpuTempCommand = (String) metrics.get("cpuTemp");
        this.cpuMemoryCommand = (String) metrics.get("cpuMemory");
        this.cpuUtilCommand = (String) metrics.get("cpuUtil");
        this.gpuMemoryCommand = (String) metrics.get("gpuMemory");
        this.gpuTempCommand = (String) metrics.get("gpuUtil");
        this.ramUtilCommand = (String) metrics.get("ramUtil");
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

    public ArrayList<Integer> getLedPins() {
        return ledPins;
    }

    public String getLedSetCommand() {
        return ledSetCommand;
    }

    public String getLedBlinkCommand() {
        return ledBlinkCommand;
    }

    public boolean isLedsCanDim() {
        return ledsCanDim;
    }

    public ArrayList<Integer> getLedPWMRange() {
        return ledPWMRange;
    }

    public String getLedPWMSetRange() {
        return ledPWMSetRange;
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
