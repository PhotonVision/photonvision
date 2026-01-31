package org.photonvision.vision.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.Point3;
import org.photonvision.common.LoadJNI;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.LogLevel;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;

public class UncertaintyTest {
    @BeforeAll
    public static void init() throws IOException {
        LoadJNI.loadLibraries();
        LoadJNI.forceLoad(LoadJNI.JNITypes.MRCAL);

        var logLevel = LogLevel.DEBUG;
        Logger.setLevel(LogGroup.Camera, logLevel);
        Logger.setLevel(LogGroup.WebServer, logLevel);
        Logger.setLevel(LogGroup.VisionModule, logLevel);
        Logger.setLevel(LogGroup.Data, logLevel);
        Logger.setLevel(LogGroup.Config, logLevel);
        Logger.setLevel(LogGroup.General, logLevel);
    }

    @Test
    public void testGenerateUncertainty() throws IOException {
        long pid = ProcessHandle.current().pid();
        System.out.println("Current Process ID (PID): " + pid);

        var cameraCal =
                new ObjectMapper()
                        .readValue(
                                Path.of(
                                                "/mnt/c/Users/matth/Downloads/photon_calibration_4c910967-fda0-4936-96af-ec4a9c969318_1280x720.json")
                                        .toFile(),
                                CameraCalibrationCoefficients.class);

        var uncertainty = cameraCal.estimateUncertainty().toArray(new Point3[0]);

        try (FileWriter f = new FileWriter("out")) {
            for (int i = 0; i < uncertainty.length; i++) {
                var p = uncertainty[i];
                f.write(p.x + "," + p.y + "," + p.z + "\n");
            }
        }
    }
}
