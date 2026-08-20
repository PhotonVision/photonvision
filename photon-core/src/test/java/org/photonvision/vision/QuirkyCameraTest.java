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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    public void logitechC270Test() {
        // Logitech's USB VID is 0x046D; the C270's PID is 0x0825. Matching is VID-first.
        QuirkyCamera c270 = QuirkyCamera.getQuirkyCamera(0x046D, 0x0825);
        assertTrue(c270.hasQuirk(CameraQuirk.CompletelyBroken));

        // The swapped (PID-first) order must NOT match
        QuirkyCamera swapped = QuirkyCamera.getQuirkyCamera(0x0825, 0x046D);
        assertFalse(swapped.hasQuirks());
    }

    @Test
    public void logitechC925eTest() {
        // Logitech's USB VID is 0x046D; the C925-e's PID is 0x085B
        QuirkyCamera c925e = QuirkyCamera.getQuirkyCamera(0x046D, 0x085B);
        assertTrue(c925e.hasQuirk(CameraQuirk.AdjustableFocus));

        QuirkyCamera swapped = QuirkyCamera.getQuirkyCamera(0x085B, 0x046D);
        assertFalse(swapped.hasQuirks());
    }

    @Test
    public void genericLogitechHdWebcamTest() {
        // ARC International's USB VID is 0x05A3; this webcam's PID is 0x9331
        QuirkyCamera hdWebcam = QuirkyCamera.getQuirkyCamera(0x05A3, 0x9331);
        assertTrue(hdWebcam.hasQuirk(CameraQuirk.CompletelyBroken));

        QuirkyCamera swapped = QuirkyCamera.getQuirkyCamera(0x9331, 0x05A3);
        assertFalse(swapped.hasQuirks());
    }

    @Test
    public void see3CamTest() {
        // e-con Systems' USB VID is 0x2560; the See3CAM_24CUG's PID is 0xC128.
        // This entry was already VID-first; pin it so it stays that way.
        QuirkyCamera see3Cam = QuirkyCamera.getQuirkyCamera(0x2560, 0xC128, "See3Cam_24CUG");
        assertTrue(see3Cam.hasQuirk(CameraQuirk.Gain));
        assertTrue(see3Cam.hasQuirk(CameraQuirk.See3Cam_24CUG));
    }
}
