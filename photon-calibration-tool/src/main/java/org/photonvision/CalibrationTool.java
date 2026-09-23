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

package org.photonvision;

import io.avaje.jsonb.JsonType;
import io.avaje.jsonb.Jsonb;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.photonvision.common.LoadJNI;
import org.photonvision.common.LoadJNI.JNITypes;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.frame.provider.SequentialFileFrameProvider;
import org.photonvision.vision.pipe.impl.Calibrate3dPipe;
import org.photonvision.vision.pipe.impl.Calibrate3dPipe.CalibratePipeParams;
import org.photonvision.vision.pipe.impl.Calibrate3dPipe.CalibrationInput;
import org.photonvision.vision.pipeline.Calibrate3dPipeline;
import org.photonvision.vision.pipeline.UICalibrationData;
import org.photonvision.vision.pipeline.UICalibrationData.BoardType;
import org.photonvision.vision.pipeline.UICalibrationData.TagFamily;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
        subcommands = {
            CalibrationTool.RedetectionCommand.class,
            CalibrationTool.RecalibrationCommand.class,
        },
        mixinStandardHelpOptions = true,
        version = "0.1 (PhotonVision " + PhotonVersion.versionString + ")")
public class CalibrationTool {
    protected static final JsonType<CameraCalibrationCoefficients> calibrationJsonb =
            Jsonb.instance().type(CameraCalibrationCoefficients.class);

    static void loadJNI() throws IOException {
        LoadJNI.loadLibraries();
        LoadJNI.forceLoad(JNITypes.MRCAL);
    }

    static class RecalibrationArgs {
        @Parameters(description = "Path to the resulting calibration")
        Path calibrationPath;

        @Parameters(description = "Estimated diagonal FOV of the camera in degrees")
        double fov;

        @Option(
                names = {"--opencv"},
                description = "Use the OpenCV backend instead of the mrCal backend")
        boolean useOpenCV;
    }

    static class CharucoArgs {
        @Option(
                names = {"--markerSize"},
                description = "Size of the markers in meters")
        double markerSizeMeters;

        @Option(
                names = {"--oldPattern"},
                description =
                        "Use the old OpenCV ChArUco pattern (whenever the top left square is a marker)")
        boolean oldPattern;

        @Option(
                names = {"--tagFamily"},
                description = "ArUco tag family, one of the following: ${COMPLETION-CANDIDATES}")
        TagFamily tagFamily;
    }

    static class BoardTypeArgs {
        @Option(names = "--chessboard", description = "Use the chessboard detector; NOT RECOMMENDED")
        boolean chessboard;

        @ArgGroup(exclusive = false)
        CharucoArgs charucoInfo;
    }

    @Command(name = "redetect", mixinStandardHelpOptions = true)
    static class RedetectionCommand implements Callable<Integer> {
        @Parameters(
                description =
                        "Directory containing images to be used for this calibration. Non-image files will be ignored.")
        Path imageDirectoryPath;

        @Mixin RecalibrationArgs rcArgs;

        @Option(
                names = {"--squareSize"},
                description = "Size of the checkerboard squares in meters")
        double squareSizeMeters;

        @Option(
                names = {"--width"},
                description = "Width of the checkerboard in squares")
        int patternWidth;

        @Option(
                names = {"--height"},
                description = "Height of the checkerboard in squares")
        int patternHeight;

        @ArgGroup BoardTypeArgs boardInfo;

        UICalibrationData makeCalibrationData() {
            return new UICalibrationData(
                    -1,
                    0,
                    squareSizeMeters,
                    boardInfo.charucoInfo != null ? boardInfo.charucoInfo.markerSizeMeters : 0.0,
                    patternWidth,
                    patternHeight,
                    boardInfo.chessboard ? BoardType.CHESSBOARD : BoardType.CHARUCOBOARD,
                    boardInfo.charucoInfo != null ? boardInfo.charucoInfo.oldPattern : false,
                    boardInfo.charucoInfo != null ? boardInfo.charucoInfo.tagFamily : null);
        }

        @Override
        public Integer call() throws IOException {
            loadJNI();
            try (SequentialFileFrameProvider provider =
                            new SequentialFileFrameProvider(imageDirectoryPath, rcArgs.fov, Integer.MAX_VALUE);
                    Calibrate3dPipeline pipeline = new Calibrate3dPipeline(); ) {
                var settings = pipeline.getSettings();
                settings.importUIData(makeCalibrationData());
                settings.resolution = provider.resolution();
                settings.useMrCal = !rcArgs.useOpenCV;

                do {
                    pipeline.takeSnapshot();
                    pipeline.run(provider.get(), null);
                } while (!provider.atStart());

                var calibration = pipeline.tryCalibration(null);
                calibrationJsonb.toJson(calibration, rcArgs.calibrationPath);
                pipeline.finishCalibration();
                System.out.println("Finished redetection!");
            }
            return 0;
        }
    }

    @Command(
            name = "recalibrate",
            description = "Recompute calibration only, reusing previously detected points.",
            mixinStandardHelpOptions = true)
    static class RecalibrationCommand implements Callable<Integer> {
        @Mixin RecalibrationArgs rcArgs;

        @Override
        public Integer call() throws IOException {
            loadJNI();
            try (Calibrate3dPipe pipe = new Calibrate3dPipe()) {
                var oldCalibration = calibrationJsonb.fromJson(rcArgs.calibrationPath);
                pipe.setParams(
                        new CalibratePipeParams(
                                (int) oldCalibration.calobjectSize.height + 1,
                                (int) oldCalibration.calobjectSize.width + 1,
                                oldCalibration.calobjectSpacing,
                                !rcArgs.useOpenCV));
                var newCalibration =
                        pipe.run(CalibrationInput.fromCalibration(oldCalibration, rcArgs.fov)).output;
                calibrationJsonb.toJson(newCalibration, rcArgs.calibrationPath);
            }
            return 0;
        }
    }

    // redetect <sourceDir> <configFile> <fov> [--opencv] --squareSize=<squareSize>
    // --width=<width> --height=<height> {--chessboard | --markerSize=<markerSize>
    // [--oldPattern] [--family={aruco_4x4 | aruco_5x5 | aruco_6x6 | aruco_7x7}]}
    // recalibrate <configFile> <fov> [--opencv]

    public static void main(String[] args) throws IOException {
        int exitCode = new CommandLine(new CalibrationTool()).execute(args);
        System.exit(exitCode);
    }
}
