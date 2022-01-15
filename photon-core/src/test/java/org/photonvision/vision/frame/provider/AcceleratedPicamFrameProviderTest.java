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

package org.photonvision.vision.frame.provider;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.opencv.imgcodecs.Imgcodecs;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.util.TestUtils;
import org.photonvision.raspi.PicamJNI;
import org.photonvision.vision.camera.ZeroCopyPicamSource;

public class AcceleratedPicamFrameProviderTest {
    @Test
    public void testGrabFrame() throws IOException {
        PicamJNI.forceLoad();
        if (!PicamJNI.isSupported()) return;

        TestUtils.loadLibraries();

        var frameProvider =
                new AcceleratedPicamFrameProvider(
                        new ZeroCopyPicamSource.PicamSettables(new CameraConfiguration("f", "f", "f", "f")));

        long lastTime = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            var frame = frameProvider.get();
            System.out.println(frame.image.getMat().get(0, 0)[0]);

            long time = System.currentTimeMillis();
            System.out.println("dt (ms): " + (time - lastTime));
            lastTime = time;
        }
        var mat = frameProvider.get().image.getMat();
        Imgcodecs.imwrite("out.png", mat);
    }
}
