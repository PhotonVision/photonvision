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

package org.photonvision.vision.processes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.photonvision.common.LoadJNI;
import org.photonvision.common.util.numbers.IntegerCouple;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipe.impl.CropPipe;
import org.photonvision.vision.pipeline.ReflectivePipelineSettings;

public class VisionRunnerTest {
    @BeforeAll
    public static void init() {
        LoadJNI.loadLibraries();
    }

    /** A crop pipe configured for the given region, via settings as the vision loop does. */
    private static CropPipe cropPipeFor(Rect rect) {
        var settings = new ReflectivePipelineSettings();
        settings.staticCropEnabled = true;
        settings.staticCropX = new IntegerCouple(rect.x, rect.x + rect.width);
        settings.staticCropY = new IntegerCouple(rect.y, rect.y + rect.height);
        var pipe = new CropPipe();
        pipe.setParams(new CropPipe.CropPipeParams(settings));
        return pipe;
    }

    /** A frame whose color image is a uniform gray, bright enough to measure dimming against. */
    private static Frame uniformFrame(int cols, int rows, int value) {
        return new Frame(
                0,
                new CVMat(new Mat(rows, cols, CvType.CV_8UC3, new Scalar(value, value, value))),
                new CVMat(new Mat(rows, cols, CvType.CV_8UC1, new Scalar(value))),
                FrameThresholdType.GREYSCALE,
                0,
                null);
    }

    @Test
    public void croppingKeepsADimmedFullFrameContextImage() {
        var frame = uniformFrame(640, 480, 200);
        var rect = new Rect(100, 50, 200, 150);

        var cropped = VisionRunner.cropFrame(cropPipeFor(rect), frame, true);

        // The pipeline's images are cropped as always.
        assertEquals(200, cropped.colorImage.getMat().cols());
        assertEquals(150, cropped.colorImage.getMat().rows());

        // The context image is the full frame: crisp inside the crop, dimmed outside it.
        var context = cropped.contextColorImage;
        assertNotNull(context, "Cropping with keepContext should produce a context image");
        assertEquals(640, context.getMat().cols());
        assertEquals(480, context.getMat().rows());

        byte[] pixel = new byte[3];
        context.getMat().get(100, 150, pixel); // inside the crop region
        assertEquals(200, pixel[0] & 0xFF, "Pixels inside the crop stay at full brightness");
        context.getMat().get(10, 10, pixel); // outside the crop region
        int dimmed = pixel[0] & 0xFF;
        assertTrue(
                dimmed > 0 && dimmed < 120, "Pixels outside the crop should be dimmed, got " + dimmed);

        // Releasing the frame releases the context image with it.
        cropped.release();
        assertTrue(context.isReleased(), "The context image is owned by the frame");
    }

    @Test
    public void croppingWithoutContextKeepsNoExtraImage() {
        var frame = uniformFrame(640, 480, 200);

        var cropped = VisionRunner.cropFrame(cropPipeFor(new Rect(100, 50, 200, 150)), frame, false);

        assertNull(cropped.contextColorImage, "No context image unless asked for");
        cropped.release();
    }
}
