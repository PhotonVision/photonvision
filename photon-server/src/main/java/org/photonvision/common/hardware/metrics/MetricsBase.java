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

import org.photonvision.common.util.ShellExec;

abstract class MetricsBase {

    private static final ShellExec runCommand = new ShellExec(true, true);

    public static double execute(String command) {
        try {
            runCommand.executeBashCommand(command);
            return Double.parseDouble(runCommand.getOutput());
        } catch (Exception e) {
            e.printStackTrace();
            return Double.NaN;
        }
    }
}
