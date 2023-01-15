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

package org.photonvision.vision.pipeline;

import edu.wpi.first.math.util.Units;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.SerializationUtils;
import org.photonvision.common.util.file.FileUtils;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipe.CVPipe.CVPipeResult;
import org.photonvision.vision.pipe.impl.CalculateFPSPipe;
import org.photonvision.vision.pipe.impl.Calibrate3dPipe;
import org.photonvision.vision.pipe.impl.FindBoardCornersPipe;
import org.photonvision.vision.pipeline.result.CVPipelineResult;

public class Calibrate3dPipeline
        extends CVPipeline<CVPipelineResult, Calibration3dPipelineSettings> {
    // For loggging
    private static final Logger logger = new Logger(Calibrate3dPipeline.class, LogGroup.General);

    // Only 2 pipes needed, one for finding the board corners and one for actually calibrating
    private final FindBoardCornersPipe findBoardCornersPipe = new FindBoardCornersPipe();
    private final Calibrate3dPipe calibrate3dPipe = new Calibrate3dPipe();
    private final CalculateFPSPipe calculateFPSPipe = new CalculateFPSPipe();

    // Getter methods have been set for calibrate and takeSnapshot
    private boolean takeSnapshot = false;

    // Output of the corners
    final List<Triple<Size, Mat, Mat>> foundCornersList;

    /// Output of the calibration, getter method is set for this.
    private CVPipeResult<CameraCalibrationCoefficients> calibrationOutput;

    private int minSnapshots;

    private boolean calibrating = false;

    // Path to save images
    private final Path imageDir = ConfigManager.getInstance().getCalibDir();

    private static final FrameThresholdType PROCESSING_TYPE = FrameThresholdType.NONE;

    public Calibrate3dPipeline() {
        this(12);
    }

    public Calibrate3dPipeline(int minSnapshots) {
        super(PROCESSING_TYPE);
        this.settings = new Calibration3dPipelineSettings();
        this.foundCornersList = new ArrayList<>();
        this.minSnapshots = minSnapshots;
    }

    @Override
    protected void setPipeParamsImpl() {
        FindBoardCornersPipe.FindCornersPipeParams findCornersPipeParams =
                new FindBoardCornersPipe.FindCornersPipeParams(
                        settings.boardHeight,
                        settings.boardWidth,
                        settings.boardType,
                        settings.gridSize,
                        settings.streamingFrameDivisor);
        findBoardCornersPipe.setParams(findCornersPipeParams);

        Calibrate3dPipe.CalibratePipeParams calibratePipeParams =
                new Calibrate3dPipe.CalibratePipeParams(
                        new Size(frameStaticProperties.imageWidth, frameStaticProperties.imageHeight));
        calibrate3dPipe.setParams(calibratePipeParams);

        // if (cameraQuirks.hasQuirk(CameraQuirk.PiCam) && LibCameraJNI.isSupported()) {
        //     LibCameraJNI.setRotation(settings.inputImageRotationMode.value);
        //     // LibCameraJNI.setShouldCopyColor(true);
        // }
    }

    @Override
    protected CVPipelineResult process(Frame frame, Calibration3dPipelineSettings settings) {
        Mat inputColorMat = frame.colorImage.getMat();

        if (this.calibrating || inputColorMat.empty()) {
            return new CVPipelineResult(0, 0, null, frame);
        }

        long sumPipeNanosElapsed = 0L;

        // Check if the frame has chessboard corners
        var outputColorCVMat = new CVMat();
        inputColorMat.copyTo(outputColorCVMat.getMat());
        var findBoardResult =
                findBoardCornersPipe.run(Pair.of(inputColorMat, outputColorCVMat.getMat())).output;

        var fpsResult = calculateFPSPipe.run(null);
        var fps = fpsResult.output;

        if (takeSnapshot) {
            // Set snapshot to false even if we don't find a board
            takeSnapshot = false;

            if (findBoardResult != null) {
                foundCornersList.add(findBoardResult);
                Imgcodecs.imwrite(
                        Path.of(imageDir.toString(), "img" + foundCornersList.size() + ".jpg").toString(),
                        inputColorMat);

                // update the UI
                broadcastState();
            }
        }

        frame.release();

        // Return the drawn chessboard if corners are found, if not, then return the input image.
        return new CVPipelineResult(
                sumPipeNanosElapsed,
                fps, // Unused but here in case
                Collections.emptyList(),
                new Frame(
                        new CVMat(), outputColorCVMat, FrameThresholdType.NONE, frame.frameStaticProperties));
    }

    public void deleteSavedImages() {
        imageDir.toFile().mkdirs();
        imageDir.toFile().mkdir();
        FileUtils.deleteDirectory(imageDir);
    }

    public boolean hasEnough() {
        return foundCornersList.size() >= minSnapshots;
    }

    public CameraCalibrationCoefficients tryCalibration() {
        if (!hasEnough()) {
            logger.info(
                    "Not enough snapshots! Only got "
                            + foundCornersList.size()
                            + " of "
                            + minSnapshots
                            + " -- returning null..");
            return null;
        }

        this.calibrating = true;

        /*Pass the board corners to the pipe, which will check again to see if all boards are valid
        and returns the corresponding image and object points*/
        calibrationOutput = calibrate3dPipe.run(foundCornersList);

        this.calibrating = false;

        return calibrationOutput.output;
    }

    public void takeSnapshot() {
        takeSnapshot = true;
    }

    public double[] perViewErrors() {
        return calibrationOutput.output.perViewErrors;
    }

    public void finishCalibration() {
        foundCornersList.forEach(
                it -> {
                    it.getMiddle().release();
                    it.getRight().release();
                });
        foundCornersList.clear();

        broadcastState();
    }

    private void broadcastState() {
        var state =
                SerializationUtils.objectToHashMap(
                        new UICalibrationData(
                                foundCornersList.size(),
                                settings.cameraVideoModeIndex,
                                minSnapshots,
                                hasEnough(),
                                Units.metersToInches(settings.gridSize),
                                settings.boardWidth,
                                settings.boardHeight,
                                settings.boardType));

        DataChangeService.getInstance()
                .publishEvent(OutgoingUIEvent.wrappedOf("calibrationData", state));
    }

    public boolean removeSnapshot(int index) {
        try {
            foundCornersList.remove(index);
            return true;
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.error("Could not remove snapshot at index " + index, e);
            return false;
        }
    }

    public CameraCalibrationCoefficients cameraCalibrationCoefficients() {
        return calibrationOutput.output;
    }
}
