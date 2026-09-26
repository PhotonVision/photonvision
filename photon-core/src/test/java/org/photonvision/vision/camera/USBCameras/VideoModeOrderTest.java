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

package org.photonvision.vision.camera.USBCameras;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.wpilib.util.PixelFormat;
import org.wpilib.vision.camera.VideoMode;

class VideoModeOrderTest {
    @Test
    void grayModesAreListedWithoutShiftingSavedModeIndices() {
        var colorModes =
                List.of(
                        new VideoMode(PixelFormat.MJPEG, 1280, 800, 120),
                        new VideoMode(PixelFormat.YUYV, 640, 480, 30),
                        new VideoMode(PixelFormat.MJPEG, 640, 480, 100),
                        new VideoMode(PixelFormat.YUYV, 1280, 800, 10));
        var grayModes =
                List.of(
                        new VideoMode(PixelFormat.GRAY, 1280, 800, 60),
                        new VideoMode(PixelFormat.GRAY, 640, 480, 100));
        var enumerated =
                List.of(
                        colorModes.get(0),
                        grayModes.get(0),
                        colorModes.get(1),
                        colorModes.get(2),
                        grayModes.get(1),
                        colorModes.get(3));

        var withoutGray = GenericUSBCameraSettables.orderVideoModes(colorModes);
        var withGray = GenericUSBCameraSettables.orderVideoModes(enumerated);

        assertEquals(withoutGray, withGray.subList(0, withoutGray.size()));
        assertEquals(
                List.of(grayModes.get(1), grayModes.get(0)),
                withGray.subList(withoutGray.size(), withGray.size()));
    }

    @Test
    void grayOnlyCameraHasUsableModes() {
        var gray = new VideoMode(PixelFormat.GRAY, 1280, 800, 60);
        assertEquals(List.of(gray), GenericUSBCameraSettables.orderVideoModes(List.of(gray, gray)));
    }
}
