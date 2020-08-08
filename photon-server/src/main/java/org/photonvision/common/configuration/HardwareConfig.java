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

    public final String deviceName;
    public final String deviceLogoPath;
    public final String supportURL;

    // LED control
    public final ArrayList<Integer> ledPins;
    public final String ledSetCommand;
    public final boolean ledsCanDim;
    public final ArrayList<Integer> ledPWMRange;
    public final String ledPWMSetRange;
    public final int ledPWMFrequency;
    public final String ledDimCommand;
    public final String ledBlinkCommand;

    // Metrics
    public final String cpuTempCommand;
    public final String cpuMemoryCommand;
    public final String cpuUtilCommand;
    public final String gpuMemoryCommand;
    public final String gpuTempCommand;
    public final String ramUtilCommand;

    // Device stuff
    public final String restartHardwareCommand;

    public HardwareConfig() {
        deviceName = "";
        deviceLogoPath = "";
        supportURL = "";
        ledPins = new ArrayList<>();
        ledSetCommand = "";
        ledsCanDim = false;
        ledPWMRange = new ArrayList<>();
        ledPWMFrequency = 0;
        ledPWMSetRange = "";
        ledDimCommand = "";

        cpuTempCommand = "";
        cpuMemoryCommand = "";
        cpuUtilCommand = "";
        gpuMemoryCommand = "";
        gpuTempCommand = "";
        ramUtilCommand = "";
        ledBlinkCommand = "";

        restartHardwareCommand = "";
    }

    @JsonCreator
    public HardwareConfig(
            @JsonProperty("deviceName") String deviceName,
            @JsonProperty("deviceLogoPath") String deviceLogoPath,
            @JsonProperty("supportURL") String supportURL,
            @JsonProperty("restartHardwareCommand") String restartHardwareCommand,
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
        this.ledPWMFrequency = (Integer) hardware.get("ledPWMFrequency");
        this.ledDimCommand = (String) hardware.get("ledDimCommand");
        this.ledBlinkCommand = (String) hardware.get("ledBlinkCommand");

        this.cpuTempCommand = (String) metrics.get("cpuTemp");
        this.cpuMemoryCommand = (String) metrics.get("cpuMemory");
        this.cpuUtilCommand = (String) metrics.get("cpuUtil");
        this.gpuMemoryCommand = (String) metrics.get("gpuMemory");
        this.gpuTempCommand = (String) metrics.get("gpuUtil");
        this.ramUtilCommand = (String) metrics.get("ramUtil");

        this.restartHardwareCommand = restartHardwareCommand;
    }
}
