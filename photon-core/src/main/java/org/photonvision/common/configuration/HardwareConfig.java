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
    public final int ledPWMFrequency;
    public final ArrayList<Integer> statusRGBPins;
    public final boolean statusRGBActiveHigh;

    // Custom GPIO
    public final String getGPIOCommand;
    public final String setGPIOCommand;
    public final String setPWMCommand;
    public final String setPWMFrequencyCommand;
    public final String releaseGPIOCommand;

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
            int ledPwmFrequency,
            ArrayList<Integer> statusRGBPins,
            boolean statusRGBActiveHigh,
            String getGPIOCommand,
            String setGPIOCommand,
            String setPWMCommand,
            String setPWMFrequencyCommand,
            String releaseGPIOCommand,
            String restartHardwareCommand,
            double vendorFOV) {
        this.deviceName = deviceName;
        this.deviceLogoPath = deviceLogoPath;
        this.supportURL = supportURL;
        this.ledPins = ledPins;
        this.ledsCanDim = ledsCanDim;
        this.ledBrightnessRange = ledBrightnessRange;
        this.ledPWMFrequency = ledPwmFrequency;
        this.statusRGBPins = statusRGBPins;
        this.statusRGBActiveHigh = statusRGBActiveHigh;
        this.getGPIOCommand = getGPIOCommand;
        this.setGPIOCommand = setGPIOCommand;
        this.setPWMCommand = setPWMCommand;
        this.setPWMFrequencyCommand = setPWMFrequencyCommand;
        this.releaseGPIOCommand = releaseGPIOCommand;
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
        ledPWMFrequency = 0;
        statusRGBPins = new ArrayList<>();
        statusRGBActiveHigh = false;
        getGPIOCommand = "";
        setGPIOCommand = "";
        setPWMCommand = "";
        setPWMFrequencyCommand = "";
        releaseGPIOCommand = "";
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
     * @return True if any gpio command has been configured to be non-empty, false otherwise
     */
    public final boolean hasGPIOCommandsConfigured() {
        return getGPIOCommand != ""
                || setGPIOCommand != ""
                || setPWMCommand != ""
                || setPWMFrequencyCommand != ""
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
                + ", ledPWMFrequency="
                + ledPWMFrequency
                + ", statusRGBPins="
                + statusRGBPins
                + ", statusRGBActiveHigh"
                + statusRGBActiveHigh
                + ", getGPIOCommand="
                + getGPIOCommand
                + ", setGPIOCommand="
                + setGPIOCommand
                + ", setPWMCommand="
                + setPWMCommand
                + ", setPWMFrequencyCommand="
                + setPWMFrequencyCommand
                + ", releaseGPIOCommand="
                + releaseGPIOCommand
                + ", restartHardwareCommand="
                + restartHardwareCommand
                + ", vendorFOV="
                + vendorFOV
                + "]";
    }
}
