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

package org.photonvision.vision.frame;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.photonvision.common.LoadJNI;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.calibration.CameraLensModel;
import org.photonvision.vision.calibration.JsonMatOfDouble;

public class FrameStaticPropertiesTest {
    private static final double EPS = 1e-6;

    @BeforeAll
    public static void init() {
        LoadJNI.loadLibraries();
    }

    @Test
    public void nullCropIsIdentity() {
        var props = new FrameStaticProperties(640, 480, 70.0, null);
        assertSame(props, props.crop(null), "A null crop rectangle should return the same instance");
    }

    @Test
    public void cropShrinksImageAndShiftsPrincipalPoint() {
        var props = new FrameStaticProperties(640, 480, 70.0, null);

        // Sanity check the un-cropped optical center (pinhole model, no calibration).
        assertEquals(319.5, props.centerX, EPS);
        assertEquals(239.5, props.centerY, EPS);

        var cropped = props.crop(new Rect(100, 50, 200, 150));

        assertEquals(200, cropped.imageWidth, "Cropped width should match the crop rectangle");
        assertEquals(150, cropped.imageHeight, "Cropped height should match the crop rectangle");
        assertEquals(200 * 150, cropped.imageArea, EPS);

        // The principal point shifts by the crop origin.
        assertEquals(319.5 - 100, cropped.centerX, EPS);
        assertEquals(239.5 - 50, cropped.centerY, EPS);

        // Cropping does not change the lens, so the focal lengths are preserved.
        assertEquals(props.horizontalFocalLength, cropped.horizontalFocalLength, EPS);
        assertEquals(props.verticalFocalLength, cropped.verticalFocalLength, EPS);
    }

    @Test
    public void cropResultIsCachedPerRectangle() {
        var props = new FrameStaticProperties(640, 480, 70.0, null);

        var rect = new Rect(10, 20, 300, 200);
        var first = props.crop(rect);
        var second = props.crop(new Rect(10, 20, 300, 200));
        assertSame(first, second, "Repeated crops with an equal rectangle should be cached");

        var third = props.crop(new Rect(0, 0, 320, 240));
        assertNotSame(first, third, "A different crop rectangle should produce a new instance");
    }

    /** A 640x480 calibration with an easily-checked principal point and focal length. */
    private static CameraCalibrationCoefficients calibration() {
        return new CameraCalibrationCoefficients(
                new Size(640, 480),
                new JsonMatOfDouble(3, 3, new double[] {600, 0, 320, 0, 600, 240, 0, 0, 1}),
                new JsonMatOfDouble(1, 5, new double[] {0.1, -0.2, 0.001, 0.002, 0.03}),
                new double[] {},
                List.of(),
                new Size(),
                1,
                CameraLensModel.LENSMODEL_OPENCV);
    }

    @Test
    public void calibratedCropShiftsIntrinsics() {
        var cal = calibration();
        var props = new FrameStaticProperties(640, 480, 70.0, cal);

        var cropped = props.crop(new Rect(100, 50, 200, 150));
        var intrinsics = cropped.cameraCalibration.cameraIntrinsics.data;

        assertEquals(600, intrinsics[0], EPS, "fx should be unchanged by a crop");
        assertEquals(600, intrinsics[4], EPS, "fy should be unchanged by a crop");
        assertEquals(320 - 100, intrinsics[2], EPS, "cx should shift by the crop origin");
        assertEquals(240 - 50, intrinsics[5], EPS, "cy should shift by the crop origin");
        assertArrayEquals(
                cal.distCoeffs.data,
                cropped.cameraCalibration.distCoeffs.data,
                "Distortion coefficients should be unchanged by a crop");
        assertEquals(
                new Size(200, 150),
                cropped.cameraCalibration.resolution,
                "The cropped calibration should describe the cropped resolution");
    }

    @Test
    public void changingCropRectangleReleasesTheSupersededCalibration() {
        var cal = calibration();
        var props = new FrameStaticProperties(640, 480, 70.0, cal);

        var first = props.crop(new Rect(0, 0, 320, 240));
        // Force the lazy native allocation that the release has to clean up.
        assertNotNull(first.cameraCalibration.getCameraIntrinsicsMat());

        var second = props.crop(new Rect(10, 10, 320, 240));
        assertNotSame(first.cameraCalibration, second.cameraCalibration);

        assertThrows(
                RuntimeException.class,
                () -> first.cameraCalibration.getCameraIntrinsicsMat(),
                "The superseded cropped calibration should have been released");
        assertNotNull(
                second.cameraCalibration.getCameraIntrinsicsMat(),
                "The current cropped calibration should still be usable");
        assertNotNull(
                cal.getCameraIntrinsicsMat(), "The camera's own calibration should not be released");
    }

    @Test
    public void disablingCropReleasesTheCachedCalibration() {
        var cal = calibration();
        var props = new FrameStaticProperties(640, 480, 70.0, cal);

        var rect = new Rect(0, 0, 320, 240);
        var cropped = props.crop(rect);
        assertNotNull(cropped.cameraCalibration.getCameraIntrinsicsMat());

        assertSame(props, props.crop(null));
        assertThrows(
                RuntimeException.class,
                () -> cropped.cameraCalibration.getCameraIntrinsicsMat(),
                "Disabling the crop should release the cached cropped calibration");

        // The cache is fully cleared, so the same rectangle produces fresh, usable properties.
        var recropped = props.crop(rect);
        assertNotSame(cropped, recropped);
        assertNotNull(recropped.cameraCalibration.getCameraIntrinsicsMat());
        assertNotNull(
                cal.getCameraIntrinsicsMat(), "The camera's own calibration should not be released");
    }
}
