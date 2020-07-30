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

public class CPU extends MetricsBase {

    private CPU() {}

    public static CPU getInstance() {
        return Singleton.INSTANCE;
    }

    public double getMemory() {
        return execute(cpuMemoryCommand);
    }

    // TODO: Command should return in Celsius
    public double getTemp() {
        return execute(cpuTemperatureCommand) / 1000;
    }

    public double getUtilization() {
        return execute(cpuUtilizationCommand);
    }

    private static class Singleton {
        public static final CPU INSTANCE = new CPU();
    }
}
