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

package org.photonvision;

import edu.wpi.first.apriltag.jni.AprilTagJNI;
import edu.wpi.first.cscore.CameraServerJNI;
import edu.wpi.first.hal.HAL;
import edu.wpi.first.hal.JNIWrapper;
import edu.wpi.first.net.WPINetJNI;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.NetworkTablesJNI;
import edu.wpi.first.util.CombinedRuntimeLoader;
import edu.wpi.first.util.WPIUtilJNI;
import edu.wpi.first.wpilibj.Timer;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.Core;
import org.photonvision.common.dataflow.structures.Packet;
import org.photonvision.jni.PhotonTargetingJniLoader;
import org.photonvision.jni.WpilibLoader;
import org.photonvision.targeting.PhotonPipelineResult;

class PhotonCameraTest {
    @BeforeAll
    public static void load_wpilib() {
        WpilibLoader.loadLibraries();
    }


    @Test
    public void testEmpty() {
        Assertions.assertDoesNotThrow(
                () -> {
                    var packet = new Packet(1);
                    var ret = new PhotonPipelineResult();
                    packet.setData(new byte[0]);
                    PhotonPipelineResult.photonStruct.pack(packet, ret);
                });
    }

    // Just a smoketest for dev use -- don't run by default
    @Test
    public void testTimeSyncServerWithPhotonCamera() throws InterruptedException, IOException {
        load_wpilib();
        PhotonTargetingJniLoader.load();

        HAL.initialize(500, 0);

        NetworkTableInstance.getDefault().stopClient();
        NetworkTableInstance.getDefault().startServer();

        var camera = new PhotonCamera("Arducam_OV2311_USB_Camera");
        PhotonCamera.setVersionCheckEnabled(false);

        for (int i = 0; i < 5; i++) {
            Thread.sleep(500);

            var res = camera.getLatestResult();
            var captureTime = res.getTimestampSeconds();
            var now = Timer.getFPGATimestamp();

            // expectTrue(captureTime < now);

            System.out.println(
                    "sequence "
                            + res.metadata.sequenceID
                            + " image capture "
                            + captureTime
                            + " received at "
                            + res.getTimestampSeconds()
                            + " now: "
                            + NetworkTablesJNI.now() / 1e6
                            + " time since last pong: "
                            + res.metadata.timeSinceLastPong / 1e6);
        }

        HAL.shutdown();
    }
}
