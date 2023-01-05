/*
 * MIT License
 *
 * Copyright (c) 2022 PhotonVision
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
import edu.wpi.first.hal.JNIWrapper;
import edu.wpi.first.net.WPINetJNI;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.NetworkTablesJNI;
import edu.wpi.first.util.CombinedRuntimeLoader;
import edu.wpi.first.util.WPIUtilJNI;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.photonvision.common.dataflow.structures.Packet;
import org.photonvision.targeting.PhotonPipelineResult;

class PhotonCameraTest {
    @Test
    public void testEmpty() {
        Assertions.assertDoesNotThrow(
                () -> {
                    var packet = new Packet(1);
                    var ret = new PhotonPipelineResult();
                    packet.setData(new byte[0]);
                    if (packet.getSize() < 1) {
                        return;
                    }
                    ret.createFromPacket(packet);
                });
    }

    @Test
    public void testFoo() throws IOException, InterruptedException {
        JNIWrapper.Helper.setExtractOnStaticLoad(false);
        WPIUtilJNI.Helper.setExtractOnStaticLoad(false);
        NetworkTablesJNI.Helper.setExtractOnStaticLoad(false);
        WPINetJNI.Helper.setExtractOnStaticLoad(false);
        AprilTagJNI.Helper.setExtractOnStaticLoad(false);

        try {
            CombinedRuntimeLoader.loadLibraries(
                    PhotonCameraTest.class,
                    "wpiutiljni",
                    "ntcorejni",
                    "wpinetjni",
                    "wpiHaljni",
                    "cscorejni",
                    "apriltagjni");
        } catch (IOException e) {
            e.printStackTrace();
        }

        var ntInstance = NetworkTableInstance.getDefault();
        ntInstance.stopServer();
        ntInstance.startClient4("photonvision");
        ntInstance.setServer("localhost");

        var camera = new PhotonCamera("foo");
        while (true) {
            var res = camera.getLatestResult();
            for (var target : res.getTargets()) {
                System.out.println(target.getDetectedCorners());
            }

            Thread.sleep(100);
        }
    }
}
