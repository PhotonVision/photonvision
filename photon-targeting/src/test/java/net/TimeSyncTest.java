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

package net;

import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wpi.first.apriltag.jni.AprilTagJNI;
import edu.wpi.first.cscore.CameraServerCvJNI;
import edu.wpi.first.cscore.CameraServerJNI;
import edu.wpi.first.hal.HAL;
import edu.wpi.first.hal.JNIWrapper;
import edu.wpi.first.math.WPIMathJNI;
import edu.wpi.first.net.WPINetJNI;
import edu.wpi.first.networktables.NetworkTablesJNI;
import edu.wpi.first.util.CombinedRuntimeLoader;
import edu.wpi.first.util.WPIUtilJNI;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.opencv.core.Core;
import org.photonvision.jni.PhotonTargetingJniLoader;
import org.photonvision.jni.TimeSyncClient;
import org.photonvision.jni.TimeSyncServer;

public class TimeSyncTest {
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
                    TimeSyncTest.class,
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
    public void smoketest() throws InterruptedException {
        try {
            load_wpilib();
            PhotonTargetingJniLoader.load();
        } catch (IOException e) {
            assertTrue(false);
        }

        HAL.initialize(1000, 0);

        var server = new TimeSyncServer(5812);
        var client = new TimeSyncClient("127.0.0.1", 5812, 1.0);

        System.err.println("Waiting: PID=" + ProcessHandle.current().pid());

        server.start();
        client.start();

        for (int i = 0; i < 5; i++) {
            Thread.sleep(1000);
            System.out.println(client.getOffset());
        }

        server.stop();
        client.stop();
    }
}
