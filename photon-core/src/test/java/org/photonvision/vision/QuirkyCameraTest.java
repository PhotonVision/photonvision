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

package org.photonvision.vision;

import java.util.HashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.photonvision.vision.camera.CameraQuirk;
import org.photonvision.vision.camera.QuirkyCamera;

public class QuirkyCameraTest {
    @Test
    public void ps3EyeTest() {
        HashMap<CameraQuirk, Boolean> ps3EyeQuirks = new HashMap<>();
        ps3EyeQuirks.put(CameraQuirk.Gain, true);
        ps3EyeQuirks.put(CameraQuirk.FPSCap100, true);
        for (var q : CameraQuirk.values()) {
            ps3EyeQuirks.putIfAbsent(q, false);
        }

        QuirkyCamera psEye = QuirkyCamera.getQuirkyCamera(0x2000, 0x1415);
        Assertions.assertEquals(psEye.quirks, ps3EyeQuirks);
    }

    @Test
    public void picamTest() {
        HashMap<CameraQuirk, Boolean> picamQuirks = new HashMap<>();
        picamQuirks.put(CameraQuirk.PiCam, true);
        for (var q : CameraQuirk.values()) {
            picamQuirks.putIfAbsent(q, false);
        }

        QuirkyCamera picam = QuirkyCamera.getQuirkyCamera(-1, -1, "mmal service 16.1");
        Assertions.assertEquals(picam.quirks, picamQuirks);
    }

    @Test
    public void quirklessCameraTest() {
        HashMap<CameraQuirk, Boolean> noQuirks = new HashMap<>();
        for (var q : CameraQuirk.values()) {
            noQuirks.put(q, false);
        }

        QuirkyCamera quirkless = QuirkyCamera.getQuirkyCamera(1234, 8888);
        Assertions.assertEquals(quirkless.quirks, noQuirks);
    }
}
