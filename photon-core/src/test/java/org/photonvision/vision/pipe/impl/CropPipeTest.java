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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.photonvision.common.LoadJNI;
import org.photonvision.common.util.numbers.IntegerCouple;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.calibration.CameraLensModel;
import org.photonvision.vision.calibration.JsonMatOfDouble;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.ImageRotationMode;
import org.photonvision.vision.pipeline.AdvancedPipelineSettings;
import org.photonvision.vision.pipeline.AprilTagPipelineSettings;
import org.photonvision.vision.pipeline.ReflectivePipelineSettings;

public class CropPipeTest {
    @BeforeAll
    public static void init() {
        LoadJNI.loadLibraries();
    }

    /** A do-nothing frame provider, to exercise the static-crop frame handling it owns. */
    private static class TestFrameProvider extends FrameProvider {
        @Override
        public Frame get() {
            return new Frame();
        }

        @Override
        protected boolean checkCameraConnected() {
            return true;
        }

        @Override
        public String getName() {
            return "TestFrameProvider";
        }

        @Override
        public void requestFrameThresholdType(FrameThresholdType type) {}

        @Override
        public void requestFrameRotation(ImageRotationMode rotationMode) {}

        @Override
        public void requestFrameCopies(boolean copyInput, boolean copyOutput) {}

        @Override
        public void requestHsvSettings(HSVPipe.HSVParams params) {}

        @Override
        public void requestBlockForFrames(boolean blockForFrames) {}

        @Override
        public void release() {}
    }

    /** A crop pipe configured to crop to the given rectangle, for a non-apriltag pipeline. */
    private static CropPipe pipeFor(Rect rect) {
        var pipe = new CropPipe();
        pipe.setParams(new CropPipe.CropPipeParams(rect, new ReflectivePipelineSettings()));
        return pipe;
    }

    /** A frame provider statically cropping to the given pixel ranges, crop enabled. */
    private static FrameProvider providerFor(int x0, int x1, int y0, int y1) {
        var provider = new TestFrameProvider();
        provider.setCropParams(settings(new IntegerCouple(x0, x1), new IntegerCouple(y0, y1)));
        return provider;
    }

    /**
     * Assert that a provider configured with the given settings statically crops a 640x480 frame to
     * exactly the expected rectangle. A pixel marked at the expected origin has to land at the
     * output's top-left corner, and the output has to have the expected size.
     */
    private static void assertStaticCrop(AdvancedPipelineSettings settings, Rect expected) {
        var provider = new TestFrameProvider();
        provider.setCropParams(settings);

        var frame = uniformFrame(640, 480, 0);
        frame.processedImage.getMat().put(expected.y, expected.x, new byte[] {(byte) 200});

        var cropped = provider.cropFrame(frame, false);
        assertEquals(expected.width, cropped.processedImage.getMat().cols(), "Crop width");
        assertEquals(expected.height, cropped.processedImage.getMat().rows(), "Crop height");

        byte[] pixel = new byte[1];
        cropped.processedImage.getMat().get(0, 0, pixel);
        assertEquals(200, pixel[0] & 0xFF, "The crop origin should be at " + expected);

        cropped.release();
    }

    /** Assert that a provider configured with the given settings leaves a 640x480 frame alone. */
    private static void assertNoStaticCrop(AdvancedPipelineSettings settings, String message) {
        var provider = new TestFrameProvider();
        provider.setCropParams(settings);

        var frame = uniformFrame(640, 480, 0);
        assertSame(frame, provider.cropFrame(frame, false), message);
        frame.release();
    }

    /** A frame whose color image is a uniform gray, bright enough to measure dimming against. */
    private static Frame uniformFrame(int cols, int rows, int value) {
        return uniformFrame(cols, rows, value, null);
    }

    /** A uniform-gray frame carrying the given static properties. */
    private static Frame uniformFrame(int cols, int rows, int value, FrameStaticProperties props) {
        return new Frame(
                0,
                new CVMat(new Mat(rows, cols, CvType.CV_8UC3, new Scalar(value, value, value))),
                new CVMat(new Mat(rows, cols, CvType.CV_8UC1, new Scalar(value))),
                FrameThresholdType.GREYSCALE,
                0,
                props);
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
    public void croppedPropertiesAreCachedPerRectangle() {
        var provider = providerFor(100, 300, 50, 200);
        var props = new FrameStaticProperties(640, 480, 70.0, null);

        var first = provider.cropFrame(uniformFrame(640, 480, 200, props), false);
        var second = provider.cropFrame(uniformFrame(640, 480, 200, props), false);
        assertSame(
                first.frameStaticProperties,
                second.frameStaticProperties,
                "An unchanged crop should reuse the derived properties");

        first.release();
        second.release();
    }

    @Test
    public void changingTheCropReleasesTheSupersededCalibration() {
        var cal = calibration();
        var props = new FrameStaticProperties(640, 480, 70.0, cal);
        var provider = providerFor(100, 300, 50, 200);

        var first = provider.cropFrame(uniformFrame(640, 480, 200, props), false);
        var firstCal = first.frameStaticProperties.cameraCalibration;
        // Force the lazy native allocation that the release has to clean up.
        assertNotNull(firstCal.getCameraIntrinsicsMat());
        first.release();

        provider.setCropParams(settings(new IntegerCouple(120, 320), new IntegerCouple(60, 210)));
        var second = provider.cropFrame(uniformFrame(640, 480, 200, props), false);
        var secondCal = second.frameStaticProperties.cameraCalibration;
        assertNotSame(firstCal, secondCal);

        assertThrows(
                RuntimeException.class,
                () -> firstCal.getCameraIntrinsicsMat(),
                "The superseded cropped calibration should have been released");
        assertNotNull(
                secondCal.getCameraIntrinsicsMat(),
                "The current cropped calibration should still be usable");
        assertNotNull(
                cal.getCameraIntrinsicsMat(), "The camera's own calibration should not be released");
        second.release();
    }

    @Test
    public void disablingTheCropReleasesTheCachedCalibration() {
        var cal = calibration();
        var props = new FrameStaticProperties(640, 480, 70.0, cal);
        var provider = providerFor(100, 300, 50, 200);

        var cropped = provider.cropFrame(uniformFrame(640, 480, 200, props), false);
        var croppedCal = cropped.frameStaticProperties.cameraCalibration;
        assertNotNull(croppedCal.getCameraIntrinsicsMat());
        cropped.release();

        var disabled = settings(new IntegerCouple(100, 300), new IntegerCouple(50, 200));
        disabled.staticCropEnabled = false;
        provider.setCropParams(disabled);
        var frame = uniformFrame(640, 480, 200, props);
        assertSame(
                frame, provider.cropFrame(frame, false), "A disabled crop should pass the frame through");

        assertThrows(
                RuntimeException.class,
                () -> croppedCal.getCameraIntrinsicsMat(),
                "Disabling the crop should release the cached cropped calibration");
        assertNotNull(
                cal.getCameraIntrinsicsMat(), "The camera's own calibration should not be released");
        frame.release();
    }

    @Test
    public void releasingThePipeReleasesTheCachedCalibration() {
        var cal = calibration();
        var props = new FrameStaticProperties(640, 480, 70.0, cal);
        var pipe = pipeFor(new Rect(100, 50, 200, 150));

        var croppedProps = pipe.croppedProperties(props, new Rect(100, 50, 200, 150));
        var croppedCal = croppedProps.cameraCalibration;
        assertNotNull(croppedCal.getCameraIntrinsicsMat());

        pipe.release();
        assertThrows(
                RuntimeException.class,
                () -> croppedCal.getCameraIntrinsicsMat(),
                "Releasing the pipe should release the cached cropped calibration");
        assertNotNull(
                cal.getCameraIntrinsicsMat(), "The camera's own calibration should not be released");
    }

    @Test
    public void cropFrameKeepsADimmedFullFrameContextImage() {
        var provider = providerFor(100, 300, 50, 200);
        var frame = uniformFrame(640, 480, 200);

        var cropped = provider.cropFrame(frame, true);

        assertEquals(200, cropped.colorImage.getMat().cols());
        assertEquals(150, cropped.colorImage.getMat().rows());

        var context = cropped.contextColorImage;
        assertNotNull(context, "Cropping with keepContext should produce a context image");
        assertEquals(640, context.getMat().cols());
        assertEquals(480, context.getMat().rows());

        byte[] pixel = new byte[3];
        context.getMat().get(100, 150, pixel);
        assertEquals(200, pixel[0] & 0xFF, "Pixels inside the crop stay at full brightness");
        context.getMat().get(10, 10, pixel);
        int dimmed = pixel[0] & 0xFF;
        assertTrue(
                dimmed > 0 && dimmed < 120, "Pixels outside the crop should be dimmed, got " + dimmed);

        cropped.release();
        assertTrue(context.isReleased(), "The context image is owned by the frame");
    }

    @Test
    public void cropFrameWithoutContextKeepsNoExtraImage() {
        var provider = providerFor(100, 300, 50, 200);
        var frame = uniformFrame(640, 480, 200);

        var cropped = provider.cropFrame(frame, false);

        assertNull(cropped.contextColorImage, "No context image unless asked for");
        cropped.release();
    }

    @Test
    public void cropReducesImageToConfiguredRegion() {
        var rect = new Rect(10, 20, 40, 30);
        CropPipe pipe = pipeFor(rect);

        CVMat in = new CVMat(new Mat(100, 100, CvType.CV_8UC1, new Scalar(42)));
        CVMat out = pipe.run(in).output;

        assertNotNull(out, "Cropping a non-empty Mat with a valid region should produce output");
        assertEquals(40, out.getMat().cols(), "Cropped width should match the configured rectangle");
        assertEquals(30, out.getMat().rows(), "Cropped height should match the configured rectangle");

        // Any non-zero entry in diff is a pixel that differs from the input's own submat.
        Mat diff = new Mat();
        Core.compare(out.getMat(), in.getMat().submat(rect), diff, Core.CMP_NE);
        boolean isIdentical = (Core.countNonZero(diff) == 0);
        diff.release();

        assertTrue(isIdentical, "Cropped output should match the equivalent submat of the input");

        out.release();
        in.release();
    }

    @Test
    public void cropSelectsTheCorrectRegion() {
        CropPipe pipe = pipeFor(new Rect(20, 10, 40, 30));

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
    public void nullRectReturnsNull() {
        CropPipe pipe = pipeFor(null);

        CVMat in = new CVMat(new Mat(100, 100, CvType.CV_8UC1, new Scalar(0)));
        CVMat out = pipe.run(in).output;

        assertNull(out, "A null crop rectangle is a no-op");

        in.release();
    }

    @Test
    public void emptyMatReturnsNull() {
        CropPipe pipe = pipeFor(new Rect(0, 0, 5, 5));

        CVMat in = new CVMat(new Mat());
        CVMat out = pipe.run(in).output;

        assertNull(out, "An empty input Mat should return null");

        in.release();
    }

    @Test
    public void setParamsStoresTheRectAndSettings() {
        var settings = new ReflectivePipelineSettings();
        var rect = new Rect(5, 6, 25, 34);
        CropPipe pipe = new CropPipe();
        pipe.setParams(new CropPipe.CropPipeParams(rect, settings));

        assertEquals(rect, pipe.getParams().rect(), "getParams should return the configured rect");
        assertEquals(
                settings, pipe.getParams().settings(), "getParams should return the configured settings");
    }

    @Test
    public void wholeFrameCropIsANoOp() {
        CropPipe pipe = pipeFor(new Rect(0, 0, 100, 100));

        CVMat in = new CVMat(new Mat(100, 100, CvType.CV_8UC1, new Scalar(42)));
        CVMat out = pipe.run(in).output;

        assertNull(out, "A crop covering the whole image should be skipped as a no-op");

        in.release();
    }

    @Test
    public void tinyCropsAreGrownToAUsableSizeInProcess() {
        // A 4x4 region grows to the 16px-per-axis minimum before the submat is taken.
        CropPipe pipe = pipeFor(new Rect(10, 10, 4, 4));

        CVMat in = new CVMat(new Mat(100, 100, CvType.CV_8UC1, new Scalar(0)));
        CVMat out = pipe.run(in).output;

        assertNotNull(out);
        assertEquals(16, out.getMat().cols());
        assertEquals(16, out.getMat().rows());

        out.release();
        in.release();
    }

    @Test
    public void staticCropFollowsMutatedSettings() {
        var settings = settings(new IntegerCouple(10, 50), new IntegerCouple(20, 50));
        var provider = new TestFrameProvider();
        provider.setCropParams(settings);

        var first = provider.cropFrame(uniformFrame(100, 100, 0), false);
        assertEquals(40, first.colorImage.getMat().cols());
        first.release();

        // The settings object is mutated in place, exactly as the settings subscriber does, so
        // the provider must rebuild the rectangle from the settings when they are handed back.
        settings.staticCropX.set(10, 90);
        provider.setCropParams(settings);

        var second = provider.cropFrame(uniformFrame(100, 100, 0), false);
        assertEquals(
                80, second.colorImage.getMat().cols(), "The crop should follow the mutated settings");
        second.release();
    }

    private static AdvancedPipelineSettings settings(IntegerCouple x, IntegerCouple y) {
        var settings = new ReflectivePipelineSettings();
        settings.staticCropEnabled = true;
        settings.staticCropX = x;
        settings.staticCropY = y;
        return settings;
    }

    /** The aligned rectangle the pipe derived for the given params, read via effectiveCrop. */
    private static Rect derivedRect(CropPipe.CropPipeParams params) {
        var pipe = new CropPipe();
        pipe.setParams(params);
        // An image large enough that clamping never interferes with the derivation under test.
        return pipe.effectiveCrop(4096, 4096);
    }

    @Test
    public void apriltagCropOriginIsAlignedToTheTileGrid() {
        // apriltag thresholds the decimated image in 4x4 tiles, so an origin off a multiple of
        // 4 * decimate moves the tiling relative to the tag and shifts the pose it reports. This
        // applies to whatever rectangle the pipe is handed: a static crop or an ML bounding box.
        var settings = new AprilTagPipelineSettings();
        var requested = new Rect(201, 151, 300, 250);

        settings.decimate = 1;
        var rect = derivedRect(new CropPipe.CropPipeParams(requested, settings));
        assertEquals(200, rect.x, "x should drop to a multiple of 4");
        assertEquals(148, rect.y, "y should drop to a multiple of 4");
        assertEquals(501, rect.x + rect.width, "The requested region should still be covered");
        assertEquals(401, rect.y + rect.height, "The requested region should still be covered");

        settings.decimate = 4;
        rect = derivedRect(new CropPipe.CropPipeParams(requested, settings));
        assertEquals(192, rect.x, "x should drop to a multiple of 16 at decimate 4");
        assertEquals(144, rect.y, "y should drop to a multiple of 16 at decimate 4");
        assertEquals(501, rect.x + rect.width, "The requested region should still be covered");
        assertEquals(401, rect.y + rect.height, "The requested region should still be covered");
    }

    @Test
    public void apriltagCropAtFrameEdgeKeepsZeroOrigin() {
        // Regression test for a bug where a zero low bound moved the aligned origin
        // past the high bound producing negative widths (e.g. Rect "-1x...").
        var settings = new AprilTagPipelineSettings();
        settings.decimate = 1;

        // Low bound touching the left edge
        var rect = derivedRect(new CropPipe.CropPipeParams(new Rect(0, 92, 837, 349), settings));
        assertEquals(0, rect.x, "x origin should remain at the frame edge");
        assertEquals(
                837, rect.x + rect.width, "The requested region's right edge should still be covered");
        assertTrue(rect.width > 0, "Width should be positive");
    }

    @Test
    public void rangesBecomeARectangle() {
        assertStaticCrop(
                settings(new IntegerCouple(10, 110), new IntegerCouple(20, 70)), new Rect(10, 20, 100, 50));
    }

    @Test
    public void reversedRangesAreNormalized() {
        assertStaticCrop(
                settings(new IntegerCouple(110, 10), new IntegerCouple(70, 20)), new Rect(10, 20, 100, 50));
    }

    @Test
    public void emptyRangesProduceNoRectangle() {
        assertNoStaticCrop(
                settings(new IntegerCouple(50, 50), new IntegerCouple(0, 100)),
                "A zero-width range means no crop");
        assertNoStaticCrop(
                settings(new IntegerCouple(0, 100), new IntegerCouple(50, 50)),
                "A zero-height range means no crop");
    }

    @Test
    public void negativeBoundsDoNotBecomeASliver() {
        // A bound that overflowed on its way in from the UI arrives as -1. Treating that as a real
        // coordinate produced a one-pixel crop, which segfaults the native apriltag detector, so the
        // sign has to be dropped instead.
        assertNoStaticCrop(
                settings(new IntegerCouple(0, -1), new IntegerCouple(0, 480)),
                "A negative upper bound should not produce a one-pixel-wide crop");
        assertNoStaticCrop(
                settings(new IntegerCouple(0, 640), new IntegerCouple(0, -1)),
                "A negative upper bound should not produce a one-pixel-tall crop");
    }

    @Test
    public void theFrameEdgeSentinelCoversTheWholeFrame() {
        // The UI's "to the frame edge" sentinel is Integer.MAX_VALUE; it has to survive as a huge
        // bound that clamps to the frame edge rather than wrapping around into something degenerate.
        assertNoStaticCrop(
                settings(new IntegerCouple(0, Integer.MAX_VALUE), new IntegerCouple(0, Integer.MAX_VALUE)),
                "A crop from the origin to the frame edge on both axes is the whole frame");
        assertStaticCrop(
                settings(
                        new IntegerCouple(100, Integer.MAX_VALUE), new IntegerCouple(50, Integer.MAX_VALUE)),
                new Rect(100, 50, 540, 430));
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
