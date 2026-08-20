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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SystemMonitorQCS6490 extends SystemMonitor {
    @Override
    protected String getThermalZoneTypes() {
        return "cpu0-thermal";
    }

    @Override
    public Map<String, Double> getNpuUsage() {
        try {
            var contents = Files.readString(Path.of("/tmp/qcnpuperf_stats"));
            Map<String, Double> map = new HashMap<>();
            Arrays.stream(contents.split("\n"))
                    .filter(line -> !line.trim().isEmpty())
                    .forEach(
                            line -> {
                                String[] parts = line.split("=");
                                if (parts.length == 2) {
                                    map.put(parts[0].trim(), Double.parseDouble(parts[1].trim()));
                                }
                            });
            return map;
        } catch (IOException e) {
            return new HashMap<>();
        }
    }
}
