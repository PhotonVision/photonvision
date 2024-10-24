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
import edu.wpi.first.cscore.CameraServerCvJNI;
import edu.wpi.first.cscore.CameraServerJNI;
import edu.wpi.first.hal.JNIWrapper;
import edu.wpi.first.math.WPIMathJNI;
import edu.wpi.first.net.WPINetJNI;
import edu.wpi.first.networktables.NetworkTablesJNI;
import edu.wpi.first.util.CombinedRuntimeLoader;
import edu.wpi.first.util.WPIUtilJNI;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opencv.core.Core;
import org.photonvision.common.dataflow.structures.Packet;
import org.photonvision.targeting.PhotonPipelineResult;

class PhotonCameraTest {
    public static void load_wpilib() {
        NetworkTablesJNI.Helper.setExtractOnStaticLoad(false);
        WPIUtilJNI.Helper.setExtractOnStaticLoad(false);
        WPIMathJNI.Helper.setExtractOnStaticLoad(false);
        CameraServerJNI.Helper.setExtractOnStaticLoad(false);
        CameraServerCvJNI.Helper.setExtractOnStaticLoad(false);
        JNIWrapper.Helper.setExtractOnStaticLoad(false);
        WPINetJNI.Helper.setExtractOnStaticLoad(false);
        AprilTagJNI.Helper.setExtractOnStaticLoad(false);

        try {
            CombinedRuntimeLoader.loadLibraries(
                    PhotonCameraTest.class,
                    "wpiutiljni",
                    "wpimathjni",
                    "ntcorejni",
                    "wpinetjni",
                    "wpiHaljni",
                    Core.NATIVE_LIBRARY_NAME,
                    "cscorejni",
                    "apriltagjni");
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    // @Test
    // public void testMeme() throws InterruptedException, IOException {
    //     load_wpilib();
    //     PhotonTargetingJniLoader.load();

    //     // HAL.initialize(500, 0);

    //     NetworkTableInstance.getDefault().stopClient();
    //     NetworkTableInstance.getDefault().startServer();

    //     var server = new TimeSyncServer(5810);
    //     server.start();

    //     var camera = new PhotonCamera("Arducam_OV9281_USB_Camera");
    //     PhotonCamera.setVersionCheckEnabled(false);

    //     for (int i = 0; i < 20; i++) {
    //         Thread.sleep(100);

    //         var res = camera.getLatestResult();
    //         var captureTime = res.getTimestampSeconds();
    //         var now = Timer.getFPGATimestamp();

    //         // expectTrue(captureTime < now);

    //         System.out.println(
    //                 "sequence "
    //                         + res.metadata.sequenceID
    //                         + " image capture "
    //                         + captureTime
    //                         + " recieved at "
    //                         + res.getNtReceiveTimestampMicros() / 1e6);
    //     }

    //     server.stop();
    // }
}
