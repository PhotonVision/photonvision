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

package org.photonvision.vision.pipe.impl;

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
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipeline.AdvancedPipelineSettings;
import org.photonvision.vision.pipeline.AprilTagPipelineSettings;
import org.photonvision.vision.pipeline.ReflectivePipelineSettings;

public class CropPipeTest {
    @BeforeAll
    public static void init() {
        LoadJNI.loadLibraries();
    }

    /** A crop pipe configured from settings describing the given pixel ranges, crop enabled. */
    private static CropPipe pipeFor(int x0, int x1, int y0, int y1) {
        var pipe = new CropPipe();
        pipe.setParams(
                new CropPipe.CropPipeParams(
                        settings(new IntegerCouple(x0, x1), new IntegerCouple(y0, y1))));
        return pipe;
    }

    @Test
    public void cropReducesImageToConfiguredRegion() {
        CropPipe pipe = pipeFor(10, 50, 20, 50);

        CVMat in = new CVMat(new Mat(100, 100, CvType.CV_8UC1, new Scalar(42)));
        CVMat out = pipe.run(in).output;

        assertNotNull(out, "Cropping a non-empty Mat with a valid region should produce output");
        assertEquals(40, out.getMat().cols(), "Cropped width should match the configured range");
        assertEquals(30, out.getMat().rows(), "Cropped height should match the configured range");

        out.release();
        in.release();
    }

    @Test
    public void cropSelectsTheCorrectRegion() {
        CropPipe pipe = pipeFor(20, 60, 10, 40);

        // A single marked pixel at (row 25, col 30) identifies the region the crop selected.
        Mat source = Mat.zeros(100, 100, CvType.CV_8UC1);
        source.put(25, 30, new byte[] {(byte) 200});
        CVMat in = new CVMat(source);
        CVMat out = pipe.run(in).output;

        assertNotNull(out);
        assertEquals(40, out.getMat().cols());
        assertEquals(30, out.getMat().rows());

        byte[] pixel = new byte[1];
        out.getMat().get(25 - 10, 30 - 20, pixel);
        assertEquals(
                200, pixel[0] & 0xFF, "The marked source pixel should map to the crop-relative position");

        out.release();
        in.release();
    }

    @Test
    public void noParamsReturnsNull() {
        CropPipe pipe = new CropPipe();

        CVMat in = new CVMat(new Mat(10, 10, CvType.CV_8UC1, new Scalar(0)));
        CVMat out = pipe.run(in).output;

        assertNull(out, "With no params configured the pipe should return null");

        in.release();
    }

    @Test
    public void disabledCropReturnsNull() {
        var settings = settings(new IntegerCouple(10, 50), new IntegerCouple(10, 50));
        settings.staticCropEnabled = false;
        CropPipe pipe = new CropPipe();
        pipe.setParams(new CropPipe.CropPipeParams(settings));

        CVMat in = new CVMat(new Mat(100, 100, CvType.CV_8UC1, new Scalar(0)));
        CVMat out = pipe.run(in).output;

        assertNull(out, "A disabled crop is a no-op");

        in.release();
    }

    @Test
    public void emptyMatReturnsNull() {
        CropPipe pipe = pipeFor(0, 5, 0, 5);

        CVMat in = new CVMat(new Mat());
        CVMat out = pipe.run(in).output;

        assertNull(out, "An empty input Mat should return null");

        in.release();
    }

    @Test
    public void setParamsStoresTheSettings() {
        var settings = settings(new IntegerCouple(5, 30), new IntegerCouple(6, 40));
        CropPipe pipe = new CropPipe();
        pipe.setParams(new CropPipe.CropPipeParams(settings));

        assertEquals(
                settings, pipe.getParams().settings(), "getParams should return the configured settings");
    }

    @Test
    public void wholeFrameCropIsANoOp() {
        CropPipe pipe = pipeFor(0, 100, 0, 100);

        CVMat in = new CVMat(new Mat(100, 100, CvType.CV_8UC1, new Scalar(42)));
        CVMat out = pipe.run(in).output;

        assertNull(out, "A crop covering the whole image should be skipped as a no-op");

        in.release();
    }

    @Test
    public void tinyCropsAreGrownToAUsableSizeInProcess() {
        // A 4x4 region grows to the 16px-per-axis minimum before the submat is taken.
        CropPipe pipe = pipeFor(10, 14, 10, 14);

        CVMat in = new CVMat(new Mat(100, 100, CvType.CV_8UC1, new Scalar(0)));
        CVMat out = pipe.run(in).output;

        assertNotNull(out);
        assertEquals(16, out.getMat().cols());
        assertEquals(16, out.getMat().rows());

        out.release();
        in.release();
    }

    @Test
    public void setParamsRederivesTheRectangle() {
        var settings = settings(new IntegerCouple(10, 50), new IntegerCouple(20, 50));
        CropPipe pipe = new CropPipe();
        pipe.setParams(new CropPipe.CropPipeParams(settings));

        CVMat in = new CVMat(new Mat(100, 100, CvType.CV_8UC1, new Scalar(0)));
        CVMat out = pipe.run(in).output;
        assertNotNull(out);
        assertEquals(40, out.getMat().cols());
        out.release();

        // The settings object is mutated in place, exactly as the settings subscriber does, so
        // setParams must re-derive the rectangle rather than trusting object equality.
        settings.staticCropX.set(10, 90);
        pipe.setParams(new CropPipe.CropPipeParams(settings));

        out = pipe.run(in).output;
        assertNotNull(out);
        assertEquals(80, out.getMat().cols(), "The rectangle should follow the mutated settings");

        out.release();
        in.release();
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

        assertNull(CropPipe.cropRectFromSettings(settings));
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
        var rect = CropPipe.cropRectFromSettings(settings);
        assertEquals(200, rect.x, "x should drop to a multiple of 4");
        assertEquals(148, rect.y, "y should drop to a multiple of 4");
        assertEquals(501, rect.x + rect.width, "The requested region should still be covered");
        assertEquals(401, rect.y + rect.height, "The requested region should still be covered");

        settings.decimate = 4;
        rect = CropPipe.cropRectFromSettings(settings);
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
                CropPipe.cropRectFromSettings(
                        settings(new IntegerCouple(10, 110), new IntegerCouple(20, 70)));

        assertEquals(new Rect(10, 20, 100, 50), rect);
    }

    @Test
    public void reversedRangesAreNormalized() {
        var rect =
                CropPipe.cropRectFromSettings(
                        settings(new IntegerCouple(110, 10), new IntegerCouple(70, 20)));

        assertEquals(new Rect(10, 20, 100, 50), rect);
    }

    @Test
    public void emptyRangesProduceNoRectangle() {
        assertNull(
                CropPipe.cropRectFromSettings(
                        settings(new IntegerCouple(50, 50), new IntegerCouple(0, 100))),
                "A zero-width range means no crop");
        assertNull(
                CropPipe.cropRectFromSettings(
                        settings(new IntegerCouple(0, 100), new IntegerCouple(50, 50))),
                "A zero-height range means no crop");
    }

    @Test
    public void negativeBoundsDoNotBecomeASliver() {
        // A bound that overflowed on its way in from the UI arrives as -1. Treating that as a real
        // coordinate produced a one-pixel crop, which segfaults the native apriltag detector, so the
        // sign has to be dropped instead.
        var x =
                CropPipe.cropRectFromSettings(
                        settings(new IntegerCouple(0, -1), new IntegerCouple(0, 480)));
        assertNull(x, "A negative upper bound should not produce a one-pixel-wide crop");

        var y =
                CropPipe.cropRectFromSettings(
                        settings(new IntegerCouple(0, 640), new IntegerCouple(0, -1)));
        assertNull(y, "A negative upper bound should not produce a one-pixel-tall crop");
    }

    @Test
    public void theFrameEdgeSentinelCoversTheWholeFrame() {
        // The UI's "to the frame edge" sentinel is Integer.MAX_VALUE; it has to survive as a huge
        // width rather than wrapping around into something degenerate.
        var rect =
                CropPipe.cropRectFromSettings(
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
        var clamped = CropPipe.clampCropToImage(new Rect(600, 400, 5000, 5000), 640, 480);

        assertEquals(600, clamped.x);
        assertEquals(400, clamped.y);
        assertEquals(40, clamped.width, "Width should stop at the right edge");
        assertEquals(80, clamped.height, "Height should stop at the bottom edge");

        // Clamping an oversized crop can leave it covering everything, which is a no-op.
        assertNull(
                CropPipe.clampCropToImage(new Rect(-50, -50, 5000, 5000), 640, 480),
                "A crop swallowing the whole image should come back as no crop");
    }

    @Test
    public void emptyAndWholeImageCropsAreNoOps() {
        assertNull(CropPipe.clampCropToImage(null, 640, 480), "A null crop is a no-op");
        assertNull(
                CropPipe.clampCropToImage(new Rect(0, 0, 0, 0), 640, 480), "An empty crop is a no-op");
        assertNull(
                CropPipe.clampCropToImage(new Rect(0, 0, 640, 480), 640, 480),
                "A crop covering the whole image is a no-op");
    }

    @Test
    public void sliverCropsAreGrownToAUsableSize() {
        // A one-pixel-tall crop crashes the native apriltag detector, so it must never reach a
        // pipeline.
        var thin = CropPipe.clampCropToImage(new Rect(0, 100, 640, 1), 640, 480);
        assertTrue(thin.height >= 16, "A one-pixel-tall crop should be grown, got " + thin);
        assertEquals(640, thin.width, "The wide axis should be left alone");

        var narrow = CropPipe.clampCropToImage(new Rect(100, 0, 1, 480), 640, 480);
        assertTrue(narrow.width >= 16, "A one-pixel-wide crop should be grown, got " + narrow);
    }

    @Test
    public void grownCropsStayInsideTheImage() {
        // A sliver against the far edge has to grow inwards, not off the end of the image.
        var corner = CropPipe.clampCropToImage(new Rect(639, 479, 1, 1), 640, 480);

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
                CropPipe.clampCropToImage(new Rect(0, 0, 2, 2), 8, 8),
                "A crop in an image smaller than the minimum should degrade to no crop");
    }
}
