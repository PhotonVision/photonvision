package org.photonvision;

import io.avaje.jsonb.JsonType;
import io.avaje.jsonb.Jsonb;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import org.photonvision.common.LoadJNI;
import org.photonvision.common.LoadJNI.JNITypes;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.frame.provider.SequentialFileFrameProvider;
import org.photonvision.vision.pipe.impl.Calibrate3dPipe;
import org.photonvision.vision.pipe.impl.Calibrate3dPipe.CalibratePipeParams;
import org.photonvision.vision.pipe.impl.Calibrate3dPipe.CalibrationInput;
import org.photonvision.vision.pipeline.Calibrate3dPipeline;
import org.photonvision.vision.pipeline.UICalibrationData;
import org.photonvision.vision.pipeline.UICalibrationData.BoardType;
import org.photonvision.vision.pipeline.UICalibrationData.TagFamily;
import org.wpilib.math.util.Units;

public class CalibrationTool {
    protected static final JsonType<CameraCalibrationCoefficients> calibrationJsonb =
            Jsonb.instance().type(CameraCalibrationCoefficients.class);

    /** Arguments for rerunning calibration, optionally also redetecting corners */
    static record CalibrationArgs(
            Path calibrationPath,
            double fov,
            boolean useMrCal,
            Optional<RedetectionArgs> redetectionArgs) {}

    /** Arguments for rerunning corner detection from a set of images */
    static record RedetectionArgs(Path imageDirectoryPath, UICalibrationData calibrationData) {}

    public static void redetect(RedetectionArgs args, double fov, Path calibrationPath) {
        try (SequentialFileFrameProvider provider =
                        new SequentialFileFrameProvider(args.imageDirectoryPath, fov, Integer.MAX_VALUE);
                Calibrate3dPipeline pipeline = new Calibrate3dPipeline(); ) {
            var settings = pipeline.getSettings();
            settings.importUIData(args.calibrationData);
            settings.resolution = provider.resolution();

            do {
                pipeline.takeSnapshot();
                pipeline.run(provider.get(), null);
            } while (!provider.atStart());

            var calibration = pipeline.tryCalibration(null);
            calibrationJsonb.toJson(calibration, calibrationPath);
            pipeline.finishCalibration();
            System.out.println("Finished redetection!");
        }
    }

    public static void recalibrate(CalibrationArgs args) {
        try (Calibrate3dPipe pipe = new Calibrate3dPipe(); ) {
            var oldCalibration = calibrationJsonb.fromJson(args.calibrationPath);
            pipe.setParams(
                    new CalibratePipeParams(
                            (int) oldCalibration.calobjectSize.height,
                            (int) oldCalibration.calobjectSize.width,
                            oldCalibration.calobjectSpacing,
                            args.useMrCal));
            var newCalibration =
                    pipe.run(CalibrationInput.fromCalibration(oldCalibration, args.fov)).output;
            calibrationJsonb.toJson(newCalibration, args.calibrationPath);
        }
    }

    public static CalibrationArgs parseArgs(String[] args) {
        // TODO: implement this stub
        return new CalibrationArgs(
                TestUtils.getResourcesFolderPath(true).resolve("calibration/lifecam_1280.json"),
                68.5,
                true,
                Optional.of(
                        new RedetectionArgs(
                                TestUtils.getResourcesFolderPath(true)
                                        .resolve("calibrationCharucoImg/lifecam/2024-05-07_lifecam_1280"),
                                new UICalibrationData(
                                        -1,
                                        0,
                                        Units.inchesToMeters(1.0),
                                        Units.inchesToMeters(0.75),
                                        8,
                                        8,
                                        BoardType.CHARUCOBOARD,
                                        false,
                                        TagFamily.Dict_4X4_1000))));
    }

    public static void main(String[] args) throws IOException {
        var parsedArgs = parseArgs(args);
        LoadJNI.loadLibraries();
        LoadJNI.forceLoad(JNITypes.MRCAL);
        if (parsedArgs.redetectionArgs.isPresent()) {
            redetect(parsedArgs.redetectionArgs.get(), parsedArgs.fov, parsedArgs.calibrationPath);
        } else {
            recalibrate(parsedArgs);
        }
    }
}
