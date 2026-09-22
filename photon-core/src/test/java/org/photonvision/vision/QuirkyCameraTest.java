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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.photonvision.vision.camera.CameraQuirk;
import org.photonvision.vision.camera.QuirkyCamera;

public class QuirkyCameraTest {
    @Test
    public void ps3EyeTest() {
        HashMap<CameraQuirk, Boolean> ps3EyeQuirks = new HashMap<>();
        ps3EyeQuirks.put(CameraQuirk.Gain, true);
        ps3EyeQuirks.put(CameraQuirk.FPSCap100, true);
        ps3EyeQuirks.put(CameraQuirk.PsEyeControls, true);
        for (var q : CameraQuirk.values()) {
            ps3EyeQuirks.putIfAbsent(q, false);
        }

        QuirkyCamera psEye = QuirkyCamera.getQuirkyCamera(0x1415, 0x2000);
        assertEquals(psEye.quirks, ps3EyeQuirks);
    }

    @Test
    public void quirklessCameraTest() {
        HashMap<CameraQuirk, Boolean> noQuirks = new HashMap<>();
        for (var q : CameraQuirk.values()) {
            noQuirks.put(q, false);
        }

        QuirkyCamera quirkless = QuirkyCamera.getQuirkyCamera(1234, 8888);
        assertEquals(quirkless.quirks, noQuirks);
    }

    @Test
    public void thriftyOv9281Test() {
        HashMap<CameraQuirk, Boolean> thriftyQuirks = new HashMap<>();
        thriftyQuirks.put(CameraQuirk.ThriftyOV9281Controls, true);
        thriftyQuirks.put(CameraQuirk.Gain, true);
        thriftyQuirks.put(CameraQuirk.MJPEGOnly, true);
        for (var q : CameraQuirk.values()) {
            thriftyQuirks.putIfAbsent(q, false);
        }

        QuirkyCamera thrifty = QuirkyCamera.getQuirkyCamera(0x1BCF, 0x28C5);
        assertEquals(thrifty.quirks, thriftyQuirks);
    }
}
