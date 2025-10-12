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

package org.photonvision.common.hardware;

import java.io.IOException;
import org.photonvision.common.util.ShellExec;

@SuppressWarnings("unused")
public class PlatformUtils {
    private static final ShellExec shell = new ShellExec(true, false);
    private static final boolean isRoot = checkForRoot();

    @SuppressWarnings("StatementWithEmptyBody")
    private static boolean checkForRoot() {
        if (Platform.isLinux()) {
            try {
                shell.executeBashCommand("id -u");
            } catch (IOException e) {
                e.printStackTrace();
            }

            while (!shell.isOutputCompleted()) {
                // TODO: add timeout
            }

            if (shell.getExitCode() == 0) {
                return shell.getOutput().split("\n")[0].equals("0");
            }

        } else {
            return true;
        }
        return false;
    }

    public static boolean isRoot() {
        return isRoot;
    }
}
