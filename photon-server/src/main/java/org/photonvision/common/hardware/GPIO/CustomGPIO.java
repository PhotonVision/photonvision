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

package org.photonvision.common.hardware.GPIO;

import java.util.ArrayList;
import java.util.List;
import org.photonvision.common.configuration.HardwareConfig;
import org.photonvision.common.hardware.Platform;

public class CustomGPIO extends GPIOBase {

    private boolean currentState;
    private List<Integer> pwmRange = new ArrayList<>();
    private int port;

    public CustomGPIO(int port) {
        this.port = port;
    }

    @Override
    public void togglePin() {
        if (this.port != -1) {
            execute(
                    commands
                            .get("setState")
                            .replace("{s}", String.valueOf(!currentState))
                            .replace("{p}", String.valueOf(this.port)));
            currentState = !currentState;
        }
    }

    @Override
    public void setLow() {
        if (this.port != -1) {
            execute(
                    commands
                            .get("setState")
                            .replace("{s}", String.valueOf(false))
                            .replace("{p}", String.valueOf(this.port)));
            currentState = false;
        }
    }

    @Override
    public void setHigh() {
        if (this.port != -1) {
            execute(
                    commands
                            .get("setState")
                            .replace("{s}", String.valueOf(true))
                            .replace("{p}", String.valueOf(this.port)));
            currentState = true;
        }
    }

    @Override
    public void setState(boolean state) {
        if (this.port != -1) {
            execute(
                    commands
                            .get("setState")
                            .replace("{s}", String.valueOf(state))
                            .replace("{p}", String.valueOf(this.port)));
            currentState = state;
        }
    }

    @Override
    public boolean shutdown() {
        if (this.port != -1) {
            execute(commands.get("shutdown"));
            return true;
        }
        return false;
    }

    @Override
    public boolean getState() {
        return currentState;
    }

    @Override
    public void setPwmRange(List<Integer> range) {
        if (this.port != -1) {
            execute(
                    commands
                            .get("setRange")
                            .replace("{lower_range}", String.valueOf(range.get(0)))
                            .replace("{upper_range}", String.valueOf(range.get(1)))
                            .replace("{p}", String.valueOf(port)));
            pwmRange = range;
        }
    }

    @Override
    public List<Integer> getPwmRange() {
        return pwmRange;
    }

    @Override
    public void blink(int pulseTimeMillis, int blinks) {
        if (this.port != -1) {
            execute(
                    commands
                            .get("blink")
                            .replace("{pulseTime}", String.valueOf(pulseTimeMillis))
                            .replace("{blinks}", String.valueOf(blinks))
                            .replace("{p}", String.valueOf(this.port)));
        }
    }

    @Override
    public void dimLED(int dimValue) {
        if (this.port != -1) {
            // Check to see if dimValue is within the range
            if (dimValue < pwmRange.get(0) || dimValue > pwmRange.get(1)) return;
            execute(
                    commands
                            .get("dim")
                            .replace("{p}", String.valueOf(port))
                            .replace("{v}", String.valueOf(dimValue)));
        }
    }

    public static void setConfig(HardwareConfig config) {
        if (Platform.isRaspberryPi()) return;
        commands.replace("setState", config.ledSetCommand);
        commands.replace("setRange", config.ledPWMSetRange);
        commands.replace("dim", config.ledDimCommand);
        commands.replace("blink", config.ledBlinkCommand);
    }
}
