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

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class QuirkyCamera {

    private static final List<QuirkyCamera> quirkyCameras =
            List.of(
                    new QuirkyCamera(0x1415, 0x2000, "PS3Eye", CameraQuirk.Gain),
                    new QuirkyCamera(0x72E, 0x45D, "LifeCam VX-5500", CameraQuirk.DoubleSet));

    public static final QuirkyCamera DefaultCamera = new QuirkyCamera(0, 0, "", List.of());

    public final int usbVid;
    public final int usbPid;
    public final String baseName;
    public final HashMap<CameraQuirk, Boolean> quirks;

    private QuirkyCamera(int usbVid, int usbPid, String baseName, CameraQuirk quirk) {
        this(usbVid, usbPid, baseName, List.of(quirk));
    }

    private QuirkyCamera(int usbVid, int usbPid, String baseName, List<CameraQuirk> quirks) {
        this.usbVid = usbVid;
        this.usbPid = usbPid;
        this.baseName = baseName;

        this.quirks = new HashMap<>();
        for (var q : quirks) {
            this.quirks.put(q, true);
        }
        for (var q : CameraQuirk.values()) {
            this.quirks.putIfAbsent(q, false);
        }
    }

    public boolean hasQuirk(CameraQuirk quirk) {
        return quirks.get(quirk);
    }

    public static QuirkyCamera getQuirkyCamera(int usbVid, int usbPid, String baseName) {
        for (var qc : quirkyCameras) {
            if (qc.usbVid == usbVid && qc.usbPid == usbPid) {
                return qc;
            }
        }
        return new QuirkyCamera(usbVid, usbPid, baseName, List.of());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuirkyCamera that = (QuirkyCamera) o;
        return usbVid == that.usbVid
                && usbPid == that.usbPid
                && Objects.equals(baseName, that.baseName)
                && Objects.equals(quirks, that.quirks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usbVid, usbPid, baseName, quirks);
    }
}
