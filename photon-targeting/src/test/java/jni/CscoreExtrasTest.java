/*
 * MIT License
 *
 * Copyright (c) PhotonVision
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package jni;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.cscore.CameraServerJNI;
import edu.wpi.first.cscore.CvSink;
import edu.wpi.first.cscore.CvSource;
import edu.wpi.first.cscore.UsbCamera;
import edu.wpi.first.hal.HAL;
import edu.wpi.first.util.PixelFormat;
import edu.wpi.first.util.RawFrame;
import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.opencv.core.Mat;
import org.photonvision.jni.CscoreExtras;
import org.photonvision.jni.PhotonTargetingJniLoader;
import org.photonvision.jni.WpilibLoader;

public class CscoreExtrasTest {
    @BeforeAll
    public static void load_wpilib() throws UnsatisfiedLinkError, IOException {
        if (!WpilibLoader.loadLibraries()) {
            fail();
        }
        if (!PhotonTargetingJniLoader.load()) {
            fail();
        }

        HAL.initialize(1000, 0);
    }

    @AfterAll
    public static void teardown() {
        HAL.shutdown();
    }

    // Skip this test for now. This doesn't work in CI anyways.
    // @Test
    public void testCaptureImage() {
        assumeTrue(CameraServerJNI.enumerateUsbCameras().length > 0);

        UsbCamera camera = CameraServer.startAutomaticCapture(2);

        camera.setVideoMode(PixelFormat.kMJPEG, 1280, 720, 30);
        var cameraMode = camera.getVideoMode();

        CvSink cvSink = CameraServer.getVideo(camera);

        CvSource outputStream = CameraServer.putVideo("Detected", 640, 480);

        long lastTime = 0;
        for (long i = 0; i < 10000000; i++) {
            var frame = new RawFrame();
            frame.setInfo(
                    cameraMode.width,
                    cameraMode.height,
                    // hard-coded 3 channel
                    cameraMode.width * 3,
                    PixelFormat.kBGR);
            final double CSCORE_DEFAULT_FRAME_TIMEOUT = 1.0 / 4.0;
            long time =
                    CscoreExtras.grabRawSinkFrameTimeoutLastTime(
                            cvSink.getHandle(), frame.getNativeObj(), CSCORE_DEFAULT_FRAME_TIMEOUT, lastTime);

            if (time != 0) {
                var mat = new Mat(CscoreExtras.wrapRawFrame(frame.getNativeObj()));

                System.out.println(mat);
                System.out.println(
                        "Mat is " + mat.cols() + " x " + mat.rows() + " (cols x rows) with type " + mat.type());

                outputStream.putFrame(mat);

                mat.release();
            } else {
                System.err.println("Sink produced an error...");
            }

            lastTime = time;

            frame.close();
        }

        cvSink.close();
        camera.close();
    }
}
