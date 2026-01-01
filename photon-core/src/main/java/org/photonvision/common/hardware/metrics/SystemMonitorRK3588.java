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

package org.photonvision.common.hardware.metrics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SystemMonitorRK3588 extends SystemMonitor {
    final String regex = "Core\\d:\\s*(\\d+)%";
    final Pattern pattern = Pattern.compile(regex);

    @Override
    protected String getThermalZoneTypes() {
        // CPU Temperature
        /* The RK3588 chip has 7 thermal zones that can be accessed via:
         *       /sys/class/thermal/thermal_zoneX/temp
         * where X is an integer from 0 to 6.
         *
         * || Zone || Location    || Comments                                                ||
         * |    0  |  soc         |  soc thermal (near the center of the chip)                |
         * |    1  |  bigcore0    |  CPU Big Core A76_0/1 (CPU4 and CPU5)                     |
         * |    2  |  bigcore1    |  CPU Big Core A76_2/3 (CPU6 and CPU7)                     |
         * |    3  |  littlecore  |  CPU Small Core A55_0/1/2/3 (CPU0, CPU1, CPU2, and CPU3)  |
         * |    4  |  center      |  also called PD_CENTER                                    |
         * |    5  |  gpu         |  GPU                                                      |
         * |    6  |  npu         |  NPU                                                      |
         *
         * Sources:
         *   - http://forum.armsom.org/t/topic/51/3
         *   - https://lore.kernel.org/lkml/7276280.TLKafQO6qx@archbook/
         */
        return "bigcore0-thermal";
    }

    @Override
    public double[] getNpuUsage() {
        try {
            var contents = Files.readString(Path.of("/sys/kernel/debug/rknpu/load"));
            Matcher matcher = pattern.matcher(contents);
            double[] results =
                    matcher.results().map(mr -> mr.group(1)).mapToDouble(Double::parseDouble).toArray();
            return results;
        } catch (IOException e) {
            return new double[0];
        }
    }
}
