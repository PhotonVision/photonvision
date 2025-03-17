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
public record HardwareConfig(
        String deviceName,
        String deviceLogoPath,
        String supportURL,
        // LED control

        ArrayList<Integer> ledPins,
        String ledSetCommand,
        boolean ledsCanDim,
        ArrayList<Integer> ledBrightnessRange,
        String ledDimCommand,
        String ledBlinkCommand,
        ArrayList<Integer> statusRGBPins,
        // Metrics

        String cpuTempCommand,
        String cpuMemoryCommand,
        String cpuUtilCommand,
        String cpuThrottleReasonCmd,
        String cpuUptimeCommand,
        String gpuMemoryCommand,
        String ramUtilCommand,
        String gpuMemUsageCommand,
        String diskUsageCommand,
        // Device stuff
        String restartHardwareCommand,
        double vendorFOV, // -1 for unmanaged
        List<Integer> blacklistedResIndices) { // this happens before the defaults are applied)

    public HardwareConfig() {
        this(
                "", // deviceName
                "", // deviceLogoPath
                "", // supportURL
                new ArrayList<>(), // ledPins
                "", // ledSetCommand
                false, // ledsCanDim
                new ArrayList<>(), // ledBrightnessRange
                "", // ledDimCommand
                "", // ledBlinkCommand
                new ArrayList<>(), // statusRGBPins
                "", // cpuTempCommand
                "", // cpuMemoryCommand
                "", // cpuUtilCommand
                "", // cpuThrottleReasonCmd
                "", // cpuUptimeCommand
                "", // gpuMemoryCommand
                "", // ramUtilCommand
                "", // gpuMemUsageCommand
                "", // diskUsageCommand
                "", // restartHardwareCommand
                -1, // vendorFOV
                Collections.emptyList()); // blacklistedResIndices
    }

    /**
     * @return True if the FOV has been preset to a sane value, false otherwise
     */
    public final boolean hasPresetFOV() {
        return vendorFOV > 0;
    }

    /**
     * @return True if any command has been configured to a non-default empty, false otherwise
     */
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
