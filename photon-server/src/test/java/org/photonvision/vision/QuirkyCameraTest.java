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

package org.photonvision.vision;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.photonvision.vision.camera.CameraQuirks;
import org.photonvision.vision.camera.QuirkyCamera;

public class QuirkyCameraTest {
    @Test
    public void ps3EyeTest() {
        QuirkyCamera psEye = new QuirkyCamera(0x1415, 0x2000, "psEye");
        Assertions.assertEquals(psEye.quirks, List.of(CameraQuirks.Gain));
    }

    @Test
    public void quirklessCameraTest() {
        QuirkyCamera noQuirk = new QuirkyCamera(1234, 888, "empty");
        Assertions.assertEquals(noQuirk.quirks, new ArrayList<>());
    }
}
