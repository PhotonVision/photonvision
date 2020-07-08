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

package org.photonvision.common.hardware.metrics;

public class GPU extends MetricsBase {

    private GPU() {}

    public static GPU getInstance() {
        return new GPU();
    }

    private static final String memoryCommand = "sudo vcgencmd get_mem gpu | grep -Eo '[0-9]+'";
    private static final String temperatureCommand =
            "sudo vcgencmd measure_temp | sed 's/[^0-9]*//g'\n";

    public double getMemory() {
        return execute(memoryCommand);
    }

    public double getTemp() {
        return execute(temperatureCommand) / 10;
    }
}
