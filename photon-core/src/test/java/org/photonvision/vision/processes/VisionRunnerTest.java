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
import org.photonvision.vision.pipe.impl.StaticCropPipe;
import org.photonvision.vision.pipeline.AdvancedPipelineSettings;
import org.photonvision.vision.pipeline.AprilTagPipelineSettings;
import org.photonvision.vision.pipeline.ReflectivePipelineSettings;

public class VisionRunnerTest {
    @BeforeAll
    public static void init() {
        LoadJNI.loadLibraries();
    }

    private static AdvancedPipelineSettings settings(IntegerCouple x, IntegerCouple y) {
        var settings = new ReflectivePipelineSettings();
        settings.staticCropEnabled = true;
        settings.staticCropX = x;
        settings.staticCropY = y;
        return settings;
    }

    @Test
    public void disabledCropProducesNoRectangle() {
        var settings = settings(new IntegerCouple(10, 100), new IntegerCouple(10, 100));
        settings.staticCropEnabled = false;

        assertNull(VisionRunner.cropRectFromSettings(settings));
    }

    @Test
    public void apriltagCropOriginIsAlignedToTheTileGrid() {
        // apriltag thresholds the decimated image in 4x4 tiles, so an origin off a multiple of
        // 4 * decimate moves the tiling relative to the tag and shifts the pose it reports.
        var settings = new AprilTagPipelineSettings();
        settings.staticCropEnabled = true;
        settings.staticCropX = new IntegerCouple(201, 501);
        settings.staticCropY = new IntegerCouple(151, 401);

        settings.decimate = 1;
        var rect = VisionRunner.cropRectFromSettings(settings);
        assertEquals(200, rect.x, "x should drop to a multiple of 4");
        assertEquals(148, rect.y, "y should drop to a multiple of 4");
        assertEquals(501, rect.x + rect.width, "The requested region should still be covered");
        assertEquals(401, rect.y + rect.height, "The requested region should still be covered");

        settings.decimate = 4;
        rect = VisionRunner.cropRectFromSettings(settings);
        assertEquals(192, rect.x, "x should drop to a multiple of 16 at decimate 4");
        assertEquals(144, rect.y, "y should drop to a multiple of 16 at decimate 4");
        assertEquals(501, rect.x + rect.width, "The requested region should still be covered");
        assertEquals(401, rect.y + rect.height, "The requested region should still be covered");
    }

    /**
     * Only the apriltag detector cares about the tiling, so other pipelines get what they asked for.
     */
    @Test
    public void rangesBecomeARectangle() {
        var rect =
                VisionRunner.cropRectFromSettings(
                        settings(new IntegerCouple(10, 110), new IntegerCouple(20, 70)));

        assertEquals(new Rect(10, 20, 100, 50), rect);
    }

    @Test
    public void reversedRangesAreNormalized() {
        var rect =
                VisionRunner.cropRectFromSettings(
                        settings(new IntegerCouple(110, 10), new IntegerCouple(70, 20)));

        assertEquals(new Rect(10, 20, 100, 50), rect);
    }

    @Test
    public void emptyRangesProduceNoRectangle() {
        assertNull(
                VisionRunner.cropRectFromSettings(
                        settings(new IntegerCouple(50, 50), new IntegerCouple(0, 100))),
                "A zero-width range means no crop");
        assertNull(
                VisionRunner.cropRectFromSettings(
                        settings(new IntegerCouple(0, 100), new IntegerCouple(50, 50))),
                "A zero-height range means no crop");
    }

    @Test
    public void negativeBoundsDoNotBecomeASliver() {
        // A bound that overflowed on its way in from the UI arrives as -1. Treating that as a real
        // coordinate produced a one-pixel crop, which segfaults the native apriltag detector, so the
        // sign has to be dropped instead.
        var x =
                VisionRunner.cropRectFromSettings(
                        settings(new IntegerCouple(0, -1), new IntegerCouple(0, 480)));
        assertNull(x, "A negative upper bound should not produce a one-pixel-wide crop");

        var y =
                VisionRunner.cropRectFromSettings(
                        settings(new IntegerCouple(0, 640), new IntegerCouple(0, -1)));
        assertNull(y, "A negative upper bound should not produce a one-pixel-tall crop");
    }

    @Test
    public void theFrameEdgeSentinelCoversTheWholeFrame() {
        // The UI's "to the frame edge" sentinel is Integer.MAX_VALUE; it has to survive as a huge
        // width rather than wrapping around into something degenerate.
        var rect =
                VisionRunner.cropRectFromSettings(
                        settings(
                                new IntegerCouple(0, Integer.MAX_VALUE), new IntegerCouple(0, Integer.MAX_VALUE)));

        assertEquals(0, rect.x);
        assertEquals(0, rect.y);
        assertEquals(Integer.MAX_VALUE, rect.width);
        assertEquals(Integer.MAX_VALUE, rect.height);
    }

    @Test
    public void cropIsClampedIntoTheImage() {
        // Overhangs the right and bottom edges, so the crop has to stop at them.
        var clamped = VisionRunner.clampCropToImage(new Rect(600, 400, 5000, 5000), 640, 480);

        assertEquals(600, clamped.x);
        assertEquals(400, clamped.y);
        assertEquals(40, clamped.width, "Width should stop at the right edge");
        assertEquals(80, clamped.height, "Height should stop at the bottom edge");

        // Clamping an oversized crop can leave it covering everything, which is a no-op.
        assertNull(
                VisionRunner.clampCropToImage(new Rect(-50, -50, 5000, 5000), 640, 480),
                "A crop swallowing the whole image should come back as no crop");
    }

    @Test
    public void emptyAndWholeImageCropsAreNoOps() {
        assertNull(VisionRunner.clampCropToImage(null, 640, 480), "A null crop is a no-op");
        assertNull(
                VisionRunner.clampCropToImage(new Rect(0, 0, 0, 0), 640, 480), "An empty crop is a no-op");
        assertNull(
                VisionRunner.clampCropToImage(new Rect(0, 0, 640, 480), 640, 480),
                "A crop covering the whole image is a no-op");
    }

    @Test
    public void sliverCropsAreGrownToAUsableSize() {
        // A one-pixel-tall crop crashes the native apriltag detector, so it must never reach a
        // pipeline.
        var thin = VisionRunner.clampCropToImage(new Rect(0, 100, 640, 1), 640, 480);
        assertTrue(thin.height >= 16, "A one-pixel-tall crop should be grown, got " + thin);
        assertEquals(640, thin.width, "The wide axis should be left alone");

        var narrow = VisionRunner.clampCropToImage(new Rect(100, 0, 1, 480), 640, 480);
        assertTrue(narrow.width >= 16, "A one-pixel-wide crop should be grown, got " + narrow);
    }

    @Test
    public void grownCropsStayInsideTheImage() {
        // A sliver against the far edge has to grow inwards, not off the end of the image.
        var corner = VisionRunner.clampCropToImage(new Rect(639, 479, 1, 1), 640, 480);

        assertTrue(corner.x >= 0 && corner.y >= 0, "Crop origin should stay in the image: " + corner);
        assertTrue(corner.x + corner.width <= 640, "Crop should end inside the image: " + corner);
        assertTrue(corner.y + corner.height <= 480, "Crop should end inside the image: " + corner);
        assertTrue(corner.width >= 16 && corner.height >= 16, "Crop should be usable: " + corner);
    }

    @Test
    public void cropsInATinyImageCannotExceedIt() {
        // The image is smaller than the minimum crop, so the crop caps out at the image -- which makes
        // it a whole-image no-op rather than a rectangle reaching past the buffer.
        assertNull(
                VisionRunner.clampCropToImage(new Rect(0, 0, 2, 2), 8, 8),
                "A crop in an image smaller than the minimum should degrade to no crop");
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

        var cropped = VisionRunner.cropFrame(new StaticCropPipe(), frame, rect, true);

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

        var cropped =
                VisionRunner.cropFrame(new StaticCropPipe(), frame, new Rect(100, 50, 200, 150), false);

        assertNull(cropped.contextColorImage, "No context image unless asked for");
        cropped.release();
    }
}
