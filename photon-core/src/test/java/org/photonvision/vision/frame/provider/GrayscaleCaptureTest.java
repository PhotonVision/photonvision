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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.photonvision.jni.LibraryLoader;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.frame.provider.USBFrameProvider.CapturePlan;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.ImageRotationMode;
import org.wpilib.util.PixelFormat;

class GrayscaleCaptureTest {
    @BeforeAll
    static void loadOpenCv() {
        assertTrue(LibraryLoader.loadWpiLibraries());
    }

    @Test
    void jpegDecodePreservesResolutionAndReleasesCompressedStorage() {
        var source = new Mat(1304, 1600, CvType.CV_8UC3, new Scalar(17, 80, 200));
        var bytes = new MatOfByte();
        assertTrue(Imgcodecs.imencode(".jpg", source, bytes));
        source.release();
        var expected = Imgcodecs.imdecode(bytes, Imgcodecs.IMREAD_GRAYSCALE);
        var encoded = new CVMat(bytes);
        try (var decoded =
                USBFrameProvider.decodeMjpegGrayscale(encoded, PixelFormat.MJPEG, 1600, 1304)) {
            assertTrue(encoded.isReleased());
            assertEquals(new Size(1600, 1304), decoded.getMat().size());
            assertEquals(CvType.CV_8UC1, decoded.getMat().type());
            assertEquals(0, Core.norm(expected, decoded.getMat(), Core.NORM_INF));
        } finally {
            expected.release();
            encoded.release();
        }
    }

    @Test
    void invalidJpegReleasesCompressedStorageAndReturnsEmptyImage() {
        var encoded = new CVMat(new MatOfByte((byte) 1, (byte) 2, (byte) 3));
        try (var decoded = USBFrameProvider.decodeMjpegGrayscale(encoded, PixelFormat.MJPEG, 3, 1)) {
            assertTrue(encoded.isReleased());
            assertTrue(decoded.getMat().empty());
        }
    }

    @Test
    void frameLeftFromPreviousFormatIsDroppedWithoutDecoding() {
        // A YUYV frame is wrapped as a 2-channel image, which imdecode would reject with an exception
        var yuyv = new CVMat(new Mat(6, 8, CvType.CV_8UC2, new Scalar(90, 128)));
        try (var decoded = USBFrameProvider.decodeMjpegGrayscale(yuyv, PixelFormat.YUYV, 8, 6)) {
            assertTrue(yuyv.isReleased());
            assertTrue(decoded.getMat().empty());
        }
    }

    @Test
    void frameFromPreviousResolutionIsScaledToCurrentMode() {
        var source = new Mat(12, 16, CvType.CV_8UC3, new Scalar(40, 40, 40));
        var bytes = new MatOfByte();
        assertTrue(Imgcodecs.imencode(".jpg", source, bytes));
        source.release();
        var encoded = new CVMat(bytes);
        try (var decoded = USBFrameProvider.decodeMjpegGrayscale(encoded, PixelFormat.MJPEG, 8, 6)) {
            assertEquals(new Size(8, 6), decoded.getMat().size());
            assertEquals(CvType.CV_8UC1, decoded.getMat().type());
        }
    }

    @Test
    void capturePlanUsesCheapestLuminanceSourceOnlyWhenGrayscaleIsRequested() {
        for (var format : PixelFormat.values()) {
            assertEquals(CapturePlan.BGR, CapturePlan.choose(false, format), format.name());
            var expected = format == PixelFormat.MJPEG ? CapturePlan.MJPEG_TO_GRAY : CapturePlan.GRAY;
            assertEquals(expected, CapturePlan.choose(true, format), format.name());
        }
        assertEquals(PixelFormat.UNKNOWN, CapturePlan.MJPEG_TO_GRAY.requestFormat);
    }

    @Test
    void grayscaleProcessingPreservesPixelsAndGeometryWithIndependentHeaders() {
        for (var rotation : ImageRotationMode.values()) {
            var input = new Mat(2, 3, CvType.CV_8UC1);
            input.put(0, 0, new byte[] {1, 2, 3, 4, 5, 6});
            var expected = input.clone();
            if (rotation != ImageRotationMode.DEG_0) {
                Core.rotate(expected, expected, rotation.value);
            }
            var properties = new FrameStaticProperties(3, 2, 70, null);
            var provider = new StubProvider(new CVMat(input), properties);
            provider.requestFrameThresholdType(FrameThresholdType.GREYSCALE);
            provider.requestFrameRotation(rotation);
            try (var frame = provider.get()) {
                assertEquals(123456789L, frame.timestampNanos);
                assertEquals(expected.cols(), frame.frameStaticProperties.imageWidth);
                assertEquals(expected.rows(), frame.frameStaticProperties.imageHeight);
                assertEquals(0, Core.norm(expected, frame.processedImage.getMat(), Core.NORM_INF));
                assertNotEquals(
                        frame.colorImage.getMat().nativeObj, frame.processedImage.getMat().nativeObj);
                // Drawing writes into the input image in place, and streaming converts and resizes it;
                // none of that may change the detector image's dimensions, channel count, or pixels.
                frame.colorImage.getMat().setTo(new Scalar(0));
                assertEquals(0, Core.norm(expected, frame.processedImage.getMat(), Core.NORM_INF));
                Imgproc.cvtColor(
                        frame.colorImage.getMat(), frame.colorImage.getMat(), Imgproc.COLOR_GRAY2BGR);
                Imgproc.resize(frame.colorImage.getMat(), frame.colorImage.getMat(), new Size(1, 1));
                assertEquals(CvType.CV_8UC1, frame.processedImage.getMat().type());
                assertEquals(expected.size(), frame.processedImage.getMat().size());
                assertEquals(0, Core.norm(expected, frame.processedImage.getMat(), Core.NORM_INF));
            } finally {
                expected.release();
                provider.release();
            }
        }
    }

    private static class StubProvider extends CpuImageProcessor {
        private final CapturedFrame input;

        StubProvider(CVMat image, FrameStaticProperties properties) {
            input = new CapturedFrame(image, properties, 123456789L);
        }

        @Override
        CapturedFrame getInputMat() {
            return input;
        }

        @Override
        public void requestGrayscaleInput(boolean grayscaleInput) {}

        @Override
        public String getName() {
            return "grayscale-test";
        }

        @Override
        protected boolean checkCameraConnected() {
            return true;
        }

        @Override
        public void release() {}
    }
}
