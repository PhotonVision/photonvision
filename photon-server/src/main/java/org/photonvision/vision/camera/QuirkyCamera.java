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

package org.photonvision.vision.camera;

import java.util.ArrayList;
import java.util.List;

public class QuirkyCamera {
    public static final List<QuirkyCamera> quirkyCameras =
            List.of(
                    // ps3 eye
                    new QuirkyCamera(0x1415, 0x2000, "PS3Eye", List.of(CameraQuirks.Gain)));

    public final int usbVid;
    public final int usbPid;
    public final String name;
    public List<CameraQuirks> quirks;

    public QuirkyCamera(int usbVid, int usbPid, String baseName, List<CameraQuirks> quirks) {
        this.usbVid = usbVid;
        this.usbPid = usbPid;
        this.name = baseName;
        this.quirks = quirks;
    }

    public QuirkyCamera(int usbVid, int usbPid, String baseName) {
        this(usbVid, usbPid, baseName, new ArrayList<>());
        QuirkyCamera quirky =
                quirkyCameras.stream()
                        .filter(quirkyCamera -> quirkyCamera.usbPid == usbPid && quirkyCamera.usbVid == usbVid)
                        .findFirst()
                        .orElse(null);
        if (quirky != null) {
            this.quirks = quirky.quirks;
        }
    }
}
