package org.photonvision.vision.pipeline;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipe.CVPipeResult;
import org.photonvision.vision.pipe.impl.Calibrate3dPipe;
import org.photonvision.vision.pipe.impl.FindBoardCornersPipe;

public class Calibrate3dPipeTest {
    @BeforeEach
    public void Init() {
        TestUtils.loadLibraries();
    }

    @Test
    public void perViewErrorsTest() {
        List<Mat> frames = new ArrayList<>();

        File dir = new File(TestUtils.getDotBoardImagesPath().toAbsolutePath().toString());
        File[] directoryListing = dir.listFiles();
        for (var file : directoryListing) {
            frames.add(Imgcodecs.imread(file.getAbsolutePath()));
        }

        FindBoardCornersPipe findBoardCornersPipe = new FindBoardCornersPipe();
        findBoardCornersPipe.setParams(
                new FindBoardCornersPipe.FindCornersPipeParams(11, 4, false, 15));
        CVPipeResult<List<List<Mat>>> findBoardCornersPipeOutput = findBoardCornersPipe.apply(frames);

        Calibrate3dPipe calibrate3dPipe = new Calibrate3dPipe();
        calibrate3dPipe.setParams(new Calibrate3dPipe.CalibratePipeParams(new Size(640, 480)));

        CVPipeResult<CameraCalibrationCoefficients> calibrate3dPipeOutput =
                calibrate3dPipe.apply(findBoardCornersPipeOutput.result);
        assertTrue(calibrate3dPipeOutput.result.perViewErrors.length > 0);
        System.out.println(
                "Per View Errors: " + Arrays.toString(calibrate3dPipeOutput.result.perViewErrors));
    }

    @Test
    public void calibrationPipelineTest() {

        File dir = new File(TestUtils.getDotBoardImagesPath().toAbsolutePath().toString());
        File[] directoryListing = dir.listFiles();

        Calibration3dPipeline calibration3dPipeline = new Calibration3dPipeline();
        calibration3dPipeline.getSettings().boardHeight = 11;
        calibration3dPipeline.getSettings().boardWidth = 4;
        calibration3dPipeline.getSettings().isUsingChessboard = false;
        calibration3dPipeline.getSettings().gridSize = 15;
        calibration3dPipeline.getSettings().resolution = new Size(640, 480);

        for (var file : directoryListing) {
            calibration3dPipeline.takeSnapshot();
            var output =
                    calibration3dPipeline.run(
                            new Frame(
                                    new CVMat(Imgcodecs.imread(file.getAbsolutePath())),
                                    new FrameStaticProperties(640, 480, 60)));
            HighGui.imshow("Calibration Output Frame", output.outputFrame.image.getMat());
        }

        calibration3dPipeline.startCalibration();
        calibration3dPipeline.run(
                new Frame(
                        new CVMat(Imgcodecs.imread(directoryListing[0].getAbsolutePath())),
                        new FrameStaticProperties(640, 480, 60)));
        System.out.println(
                "Per View Errors: " + Arrays.toString(calibration3dPipeline.perViewErrors()));
        System.out.println(
                "Camera Intrinsics : "
                        + calibration3dPipeline.cameraCalibrationCoefficients().cameraIntrinsics.toString());
        System.out.println(
                "Camera Extrinsics : "
                        + calibration3dPipeline.cameraCalibrationCoefficients().cameraExtrinsics.toString());
    }
}
