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

package org.photonvision.common.hardware.PWM;

public class CustomPWM extends PWMBase {
    private int pwmRate = 0;
    private int pwmRange = 0;
    private int port = 0;

    public CustomPWM(int port) {
        this.port = port;
    }

    @Override
    public void setPwmRate(int rate) {
        execute(
                commands
                        .get("setRate")
                        .replace("{rate}", String.valueOf(rate))
                        .replace("{p}", String.valueOf(port)));
        pwmRate = rate;
    }

    @Override
    public void setPwmRange(int range) {
        execute(
                commands
                        .get("setRange")
                        .replace("{range}", String.valueOf(range))
                        .replace("{p}", String.valueOf(port)));
        pwmRange = range;
    }

    @Override
    public int getPwmRate() {
        return pwmRate;
    }

    @Override
    public int getPwmRange() {
        return pwmRange;
    }

    @Override
    public boolean shutdown() {
        execute(commands.get("shutdown"));
        return true;
    }
}
