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

import org.photonvision.common.configuration.HardwareConfig;

public class CustomGPIO extends GPIOBase {

    private boolean currentState;
    private int port;

    public CustomGPIO(int port) {
        this.port = port;
    }

    @Override
    public void togglePin() {
        execute(
                commands
                        .get("setState")
                        .replace("{s}", String.valueOf(!currentState))
                        .replace("{p}", String.valueOf(this.port)));
        currentState = !currentState;
    }

    @Override
    public void setLow() {
        execute(
                commands
                        .get("setState")
                        .replace("{s}", String.valueOf(false))
                        .replace("{p}", String.valueOf(this.port)));
        currentState = false;
    }

    @Override
    public void setHigh() {
        execute(
                commands
                        .get("setState")
                        .replace("{s}", String.valueOf(true))
                        .replace("{p}", String.valueOf(this.port)));
        currentState = true;
    }

    @Override
    public void setState(boolean state) {
        execute(
                commands
                        .get("setState")
                        .replace("{s}", String.valueOf(state))
                        .replace("{p}", String.valueOf(this.port)));
        currentState = state;
    }

    @Override
    public void blink(long delay, long duration) {
        execute(
                commands
                        .get("blink")
                        .replace("{delay}", String.valueOf(delay))
                        .replace("{duration}", String.valueOf(duration))
                        .replace("{p}", String.valueOf(this.port)));
    }

    @Override
    public void pulse(long duration, boolean blocking) {
        execute(
                commands
                        .get("pulse")
                        .replace("{blocking}", String.valueOf(blocking))
                        .replace("{duration}", String.valueOf(duration))
                        .replace("{p}", String.valueOf(this.port)));
    }

    @Override
    public boolean shutdown() {
        execute(commands.get("shutdown"));
        return true;
    }

    @Override
    public boolean getState() {
        return currentState;
    }

    public static void setConfig(HardwareConfig config) {
        commands.replace("setState", config.getLedSetCommand());
        commands.replace("blink", config.getLedBlinkCommand());
        commands.replace("pulse", config.getLedPulseCommand());
    }
}
