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
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.SerializationUtils;
import org.photonvision.vision.calibration.BoardObservation;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.ImageRotationMode;
import org.photonvision.vision.pipe.CVPipe.CVPipeResult;
import org.photonvision.vision.pipe.impl.CalculateFPSPipe;
import org.photonvision.vision.pipe.impl.Calibrate3dPipe;
import org.photonvision.vision.pipe.impl.Calibrate3dPipe.CalibrationInput;
import org.photonvision.vision.pipe.impl.FindBoardCornersPipe;
import org.photonvision.vision.pipe.impl.FindBoardCornersPipe.FindBoardCornersPipeResult;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.pipeline.result.CalibrationPipelineResult;

public class Calibrate3dPipeline
        extends CVPipeline<CVPipelineResult, Calibration3dPipelineSettings> {
    // For logging
    private static final Logger logger = new Logger(Calibrate3dPipeline.class, LogGroup.General);

    // Find board corners decides internally between opencv and mrgingham
    private final FindBoardCornersPipe findBoardCornersPipe = new FindBoardCornersPipe();
    private final Calibrate3dPipe calibrate3dPipe = new Calibrate3dPipe();
    private final CalculateFPSPipe calculateFPSPipe = new CalculateFPSPipe();

    // Getter methods have been set for calibrate and takeSnapshot
    private boolean takeSnapshot = false;

    // Output of the corners
    public final List<FindBoardCornersPipeResult> foundCornersList;

    /// Output of the calibration, getter method is set for this.
    private CVPipeResult<CameraCalibrationCoefficients> calibrationOutput;

    private final int minSnapshots;

    private boolean calibrating = false;

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
                        settings.tagFamily,
                        settings.gridSize,
                        settings.markerSize,
                        settings.streamingFrameDivisor,
                        settings.useOldPattern);
        findBoardCornersPipe.setParams(findCornersPipeParams);

        Calibrate3dPipe.CalibratePipeParams calibratePipeParams =
                new Calibrate3dPipe.CalibratePipeParams(
                        settings.boardHeight, settings.boardWidth, settings.gridSize, settings.useMrCal);
        calibrate3dPipe.setParams(calibratePipeParams);
    }

    @Override
    protected CVPipelineResult process(Frame frame, Calibration3dPipelineSettings settings) {
        Mat inputColorMat = frame.colorImage.getMat();

        if (this.calibrating || inputColorMat.empty()) {
            return new CVPipelineResult(frame.sequenceID, 0, 0, null, frame);
        }

        if (getSettings().inputImageRotationMode != ImageRotationMode.DEG_0) {
            // All this calibration assumes zero rotation. If we want a rotation, it should
            // be applied at
            // the output
            logger.error(
                    "Input image rotation was non-zero! Calibration wasn't designed to deal with this. Attempting to manually change back to zero");
            getSettings().inputImageRotationMode = ImageRotationMode.DEG_0;
            return new CVPipelineResult(frame.sequenceID, 0, 0, List.of(), frame);
        }

        long sumPipeNanosElapsed = 0L;

        // Check if the frame has chessboard corners
        var outputColorCVMat = new CVMat();
        inputColorMat.copyTo(outputColorCVMat.getMat());

        FindBoardCornersPipeResult findBoardResult;

        findBoardResult =
                findBoardCornersPipe.run(Pair.of(inputColorMat, outputColorCVMat.getMat())).output;

        if (takeSnapshot) {
            // Set snapshot to false even if we don't find a board
            takeSnapshot = false;

            if (findBoardResult != null) {
                // Only copy the image into the result when we absolutely must
                findBoardResult.inputImage = inputColorMat.clone();

                foundCornersList.add(findBoardResult);

                // update the UI
                broadcastState();
            }
        }

        var fpsResult = calculateFPSPipe.run(null);
        var fps = fpsResult.output;

        frame.release();

        // Return the drawn chessboard if corners are found, if not, then return the
        // input image.
        return new CalibrationPipelineResult(
                frame.sequenceID,
                sumPipeNanosElapsed,
                fps, // Unused but here in case
                new Frame(
                        frame.sequenceID,
                        new CVMat(),
                        outputColorCVMat,
                        FrameThresholdType.NONE,
                        frame.frameStaticProperties),
                getCornersList());
    }

    List<List<Point>> getCornersList() {
        return foundCornersList.stream()
                .map(it -> it.imagePoints.toList())
                .collect(Collectors.toList());
    }

    public boolean hasEnough() {
        return foundCornersList.size() >= minSnapshots;
    }

    public CameraCalibrationCoefficients tryCalibration(Path imageSavePath) {
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

        /*
         * Pass the board corners to the pipe, which will check again to see if all
         * boards are valid
         * and returns the corresponding image and object points
         */
        calibrationOutput =
                calibrate3dPipe.run(
                        new CalibrationInput(foundCornersList, frameStaticProperties, imageSavePath));

        this.calibrating = false;

        return calibrationOutput.output;
    }

    public void takeSnapshot() {
        takeSnapshot = true;
    }

    public List<BoardObservation> perViewErrors() {
        return calibrationOutput.output.observations;
    }

    public void finishCalibration() {
        foundCornersList.forEach(it -> it.release());
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
                                Units.metersToInches(settings.markerSize),
                                settings.boardWidth,
                                settings.boardHeight,
                                settings.boardType,
                                settings.useOldPattern,
                                settings.tagFamily));

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

    @Override
    public void release() {
        // we never actually need to give resources up since pipelinemanager only makes
        // one of us
    }
}
