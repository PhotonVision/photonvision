package net;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.opencv.core.Core;
import org.photonvision.jni.TimeSyncClient;
import org.photonvision.jni.TimeSyncJNILoader;
import org.photonvision.jni.TimeSyncServer;

import edu.wpi.first.apriltag.jni.AprilTagJNI;
import edu.wpi.first.cscore.CameraServerCvJNI;
import edu.wpi.first.cscore.CameraServerJNI;
import edu.wpi.first.hal.JNIWrapper;
import edu.wpi.first.math.WPIMathJNI;
import edu.wpi.first.net.WPINetJNI;
import edu.wpi.first.networktables.NetworkTablesJNI;
import edu.wpi.first.util.CombinedRuntimeLoader;
import edu.wpi.first.util.WPIUtilJNI;

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

        // for (var line : System.getProperty("java.class.path", ".").split(";")) {
        //     System.out.println(line);
        // }

        try {
            load_wpilib();   
            TimeSyncJNILoader.load();
        } catch (IOException e) {
            assertTrue(false);
        }

        // var server = new TimeSyncServer(5812);
        var client = new TimeSyncClient("127.0.0.1", 5812, 1.0);

        // server.start();
        client.start();

        for (int i = 0; i < 5; i++) {
            Thread.sleep(1000);
            // System.out.println(client.getOffset());
        }

        // server.stop();
        client.stop();
    }
}
