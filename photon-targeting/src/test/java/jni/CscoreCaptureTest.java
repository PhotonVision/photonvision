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

package jni;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.photonvision.jni.CscoreExtras;
import org.photonvision.jni.LibraryLoader;
import org.wpilib.util.PixelFormat;
import org.wpilib.util.RawFrame;
import org.wpilib.vision.camera.CvSink;
import org.wpilib.vision.camera.VideoException;
import org.wpilib.vision.camera.raw.RawSource;

/** Exercises the actual CSCore capture and JNI wrapper without requiring a USB camera. */
class CscoreCaptureTest {
    @BeforeAll
    static void loadNatives() {
        assertTrue(LibraryLoader.loadWpiLibraries());
        assertTrue(LibraryLoader.loadTargeting());
    }

    @Test
    void grabErrorRaisesVideoExceptionInsteadOfCrashing() {
        int closedSink;
        try (var sink = new CvSink("closed-sink")) {
            closedSink = sink.getHandle();
        }
        try (var frame = new RawFrame()) {
            var error =
                    assertThrows(
                            VideoException.class,
                            () ->
                                    CscoreExtras.grabRawSinkFrameTimeoutLastTime(
                                            closedSink, frame.getNativeObj(), 0.01, 0));
            assertEquals("invalid handle", error.getMessage());
        }
    }

    @Test
    void originalFormatCaptureReturnsCompressedJpegThroughCvSink() {
        int width = 1600;
        int height = 1304;
        var sourceImage = new Mat(height, width, CvType.CV_8UC3, new Scalar(17, 80, 200));
        var encoded = new MatOfByte();
        byte[] jpeg;
        try {
            assertTrue(Imgcodecs.imencode(".jpg", sourceImage, encoded));
            jpeg = encoded.toArray();
        } finally {
            sourceImage.release();
            encoded.release();
        }

        var data = ByteBuffer.allocateDirect(jpeg.length);
        data.put(jpeg).flip();
        try (var source =
                        new RawSource("jpeg-regression-source", PixelFormat.MJPEG, width, height, 30);
                var sink = new CvSink("jpeg-regression-sink");
                var captured = new RawFrame()) {
            source.setConnected(true);
            sink.setSource(source);
            sink.setEnabled(true);
            source.putFrame(data, width, height, 0, PixelFormat.MJPEG);

            // UNKNOWN returns the original bytes. Requesting MJPEG instead asks CSCore's
            // GetImage(), which rejects compressed output and silently returns no frame.
            captured.setInfo(width, height, 0, PixelFormat.UNKNOWN);
            long timestamp =
                    CscoreExtras.grabRawSinkFrameTimeoutLastTime(
                            sink.getHandle(), captured.getNativeObj(), 0.25, -1);
            assertTrue(timestamp > 0, "A published JPEG must be available without a second frame");
            assertEquals(PixelFormat.MJPEG, CscoreExtras.getPixelFormat(captured));

            var wrapped = new Mat(CscoreExtras.wrapRawFrame(captured.getNativeObj()));
            Mat decoded = null;
            try {
                assertEquals(1, wrapped.rows());
                assertEquals(jpeg.length, wrapped.cols());
                assertEquals(CvType.CV_8UC1, wrapped.type());
                byte[] actual = new byte[jpeg.length];
                wrapped.get(0, 0, actual);
                assertArrayEquals(jpeg, actual);

                decoded = Imgcodecs.imdecode(wrapped, Imgcodecs.IMREAD_GRAYSCALE);
                assertEquals(width, decoded.cols());
                assertEquals(height, decoded.rows());
                assertEquals(CvType.CV_8UC1, decoded.type());
            } finally {
                if (decoded != null) decoded.release();
                wrapped.release();
            }
        }
    }
}
