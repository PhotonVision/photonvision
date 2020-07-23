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

import java.util.ArrayList;
import java.util.List;
import org.photonvision.common.configuration.HardwareConfig;
import org.photonvision.common.hardware.Platform;

public class CustomPWM extends PWMBase {
    private List<Integer> pwmRange = new ArrayList<>();
    private int port = 0;

    public CustomPWM(int port) {
        this.port = port;
    }

    @Override
    public void setPwmRange(List<Integer> range) {
        execute(
                commands
                        .get("setRange")
                        .replace("{lower_range}", String.valueOf(range.get(0)))
                        .replace("{upper_range}", String.valueOf(range.get(1)))
                        .replace("{p}", String.valueOf(port)));
        pwmRange = range;
    }

    @Override
    public List<Integer> getPwmRange() {
        return pwmRange;
    }

    @Override
    public boolean shutdown() {
        execute(commands.get("shutdown"));
        return true;
    }

    @Override
    public void dimLED(int dimValue) {
        // Check to see if dimValue is within the range
        if (dimValue < pwmRange.get(0) || dimValue > pwmRange.get(1)) return;
        execute(
                commands
                        .get("dim")
                        .replace("{p}", String.valueOf(port))
                        .replace("{v}", String.valueOf(dimValue)));
    }

    public static void setConfig(HardwareConfig config) {
        if (Platform.isRaspberryPi()) return;
        commands.replace("setRange", config.ledPWMSetRange);
        commands.replace("dim", config.ledDimCommand);
    }
}
