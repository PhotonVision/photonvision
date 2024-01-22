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

package org.photonvision.vision.pipe.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
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
import org.photonvision.vision.pipe.impl.FindBoardCornersGuidancePipe.FindBoardCornersGuidancePipeResult;
import org.photonvision.vision.pipe.impl.FindBoardCornersPipe.FindBoardCornersPipeResult;
import org.photonvision.vision.pipeline.CVPipeline;
import org.photonvision.vision.pipeline.Calibration3dPipelineSettings;
import org.photonvision.vision.pipeline.UICalibrationData;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.pipeline.result.CalibrationPipelineResult;

import edu.wpi.first.math.util.Units;

//FIXME TBD - MrCal requirements and change Calibrate3dPipe.CalibratePipeParams as needed for ChArUcoBoard properties
// FindBoardCornersGuidancePipeResults is different than FindBoardCornersResults
// for now FindBoardCornersParams used by FindBoardCornersPipe is okay (ignored) for FindBoardCornersGuidancePipe

public class Calibrate3dPipeline
        extends CVPipeline<CVPipelineResult, Calibration3dPipelineSettings> {
    // For logging
    private static final Logger logger = new Logger(Calibrate3dPipeline.class, LogGroup.General);

    //TODO make providePoseGuidance a user input button
    private static final boolean providePoseGuidance = true; // true suppresses legacy manual pose determination

    // Find board corners decides internally between opencv and mrgingham
    private FindBoardCornersPipe findBoardCornersPipe;
    private FindBoardCornersGuidancePipe findBoardCornersGuidancePipe;

    private final Calibrate3dPipe calibrate3dPipe = new Calibrate3dPipe();
    private final CalculateFPSPipe calculateFPSPipe = new CalculateFPSPipe();

    // Getter methods have been set for calibrate and takeSnapshot
    private boolean takeSnapshot = false;

    // Output of the corners
    public final List<FindBoardCornersPipeResult> foundCornersList;

    /// Output of the calibration, getter method is set for this.
    private CVPipeResult<CameraCalibrationCoefficients> calibrationOutput;

    private int minSnapshots;

    private boolean calibrating = false;

    private static final FrameThresholdType PROCESSING_TYPE = FrameThresholdType.NONE;

    public Calibrate3dPipeline(String uniqueName) {
        this(12, uniqueName);
        // runs once when PV starts
    }

    public Calibrate3dPipeline(int minSnapshots, String uniqueName) { // runs once when PV starts
        super(PROCESSING_TYPE);
        this.settings = new Calibration3dPipelineSettings();
        this.foundCornersList = new ArrayList<>();
        this.minSnapshots = minSnapshots;
    }

    @Override
    protected void setPipeParamsImpl() {
        // runs first once per frame after calibration has started until calibration ends/canceled
        // first time through check for this calibration session
        if (providePoseGuidance) {
            if (findBoardCornersGuidancePipe == null) {
                findBoardCornersGuidancePipe = new FindBoardCornersGuidancePipe();
                //TODO make guidance its own params (not needed now) instead of copying unneeded stuff from FindCornersPipeParams
                FindBoardCornersGuidancePipe.FindCornersGuidancePipeParams findCornersGuidancePipeParams =
                        new FindBoardCornersGuidancePipe.FindCornersGuidancePipeParams(
                                settings.boardHeight,
                                settings.boardWidth,
                                settings.boardType,
                                settings.gridSize,
                                settings.streamingFrameDivisor);
                findBoardCornersGuidancePipe.setParams(findCornersGuidancePipeParams);
            }
        }
        else {
            if (findBoardCornersPipe == null) {
                findBoardCornersPipe = new FindBoardCornersPipe();
                FindBoardCornersPipe.FindCornersPipeParams findCornersPipeParams =
                        new FindBoardCornersPipe.FindCornersPipeParams(
                                settings.boardHeight,
                                settings.boardWidth,
                                settings.boardType,
                                settings.gridSize,
                                settings.streamingFrameDivisor);
                findBoardCornersPipe.setParams(findCornersPipeParams);
            }
        }

        //TODO MrCal needs the ChArUcoBoard parameters similar to other boards

        Calibrate3dPipe.CalibratePipeParams calibratePipeParams =
                new Calibrate3dPipe.CalibratePipeParams(
                        settings.boardHeight, settings.boardWidth, settings.gridSize, settings.useMrCal);
        calibrate3dPipe.setParams(calibratePipeParams);
    }

    @Override
    protected CVPipelineResult process(Frame frame, Calibration3dPipelineSettings settings) {
        // runs second once per frame after calibration has started until calibration ends/canceled

        Mat inputColorMat = frame.colorImage.getMat();

        if (this.calibrating || inputColorMat.empty()) {
            return new CVPipelineResult(0, 0, null, frame);
        }

        if (getSettings().inputImageRotationMode != ImageRotationMode.DEG_0) {
            // All this calibration assumes zero rotation. If we want a rotation, it should be applied at
            // the output
            logger.error(
                    "Input image rotation was non-zero! Calibration wasn't designed to deal with this. Attempting to manually change back to zero");
            getSettings().inputImageRotationMode = ImageRotationMode.DEG_0;
            return new CVPipelineResult(0, 0, List.of(), frame);
        }

        long sumPipeNanosElapsed = 0L;

        // Check if the frame has chessboard corners
        var outputColorCVMat = new CVMat();
        inputColorMat.copyTo(outputColorCVMat.getMat());
        
        FindBoardCornersPipeResult findBoardResult = null;

        if (providePoseGuidance) {
            FindBoardCornersGuidancePipeResult findBoardGuidanceResult =
                findBoardCornersGuidancePipe.run(Pair.of(inputColorMat, outputColorCVMat.getMat())).output;

            //FIXME need to handle CANCEL to bail out and ENOUGH needs to run calibrate after taking the snapshot

            if (findBoardGuidanceResult.takeSnapshot || findBoardGuidanceResult.haveEnough || findBoardGuidanceResult.cancelCalibration) {
                logger.debug("guidance result for variables 'snapshot', 'enough', and 'cancel':"
                    + findBoardGuidanceResult.takeSnapshot
                    + findBoardGuidanceResult.haveEnough
                    + findBoardGuidanceResult.cancelCalibration);
            }

            if (findBoardGuidanceResult.haveEnough) {
                minSnapshots = 0;
            }
            // findBoardGuidanceResult.takeSnapshot frame captured; maybe we don't care since it'll come back at us at the end when calibrated
            // findBoardGuidanceResult.haveEnough calibrated; done; null the guidance for fresh start next time
            // findBoardGuidanceResult.cancelCalibration no data; null the guidance for fresh start next frame

            takeSnapshot = findBoardGuidanceResult.takeSnapshot;

            if (takeSnapshot) {
                // convert guidance result to non-guidance results
                //FIXME will need the corner ids, too, when ChArUcoBoard added to calibrate for MrCal
                MatOfPoint3f temp1 = new MatOfPoint3f();
                MatOfPoint2f temp2 = new MatOfPoint2f();
                findBoardGuidanceResult.objCorners.copyTo(temp1);
                findBoardGuidanceResult.imgCorners.copyTo(temp2);
                findBoardResult =
                    new FindBoardCornersPipeResult(
                        findBoardGuidanceResult.imgSize,
                        temp1,
                        temp2);
            }    
        }
        else {    
            findBoardResult =
                findBoardCornersPipe.run(Pair.of(inputColorMat, outputColorCVMat.getMat())).output;
        }
 
        var fpsResult = calculateFPSPipe.run(null);
        var fps = fpsResult.output;

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

        frame.release();

        // Return the drawn chessboard if corners are found, if not, then return the input image.
        return new CalibrationPipelineResult(
                sumPipeNanosElapsed,
                fps, // Unused but here in case
                new Frame(
                        new CVMat(), outputColorCVMat, FrameThresholdType.NONE, frame.frameStaticProperties),
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

    public CameraCalibrationCoefficients tryCalibration() {
        logger.debug("RKT tryCalibration");
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

        if ( providePoseGuidance) {
            findBoardCornersGuidancePipe = null; // this calibration session done so reset guidance to start afresh next frame        
        }

        return calibrationOutput.output;
    }

    public void takeSnapshot() {
        logger.debug("RKT takeSnapshot");
        takeSnapshot = true;
    }

    public List<BoardObservation> perViewErrors() {
        logger.debug("RKT perViewErrors");
        return calibrationOutput.output.observations;
    }

    public void finishCalibration() {
        logger.debug("RKT finishCalibraton");
        foundCornersList.forEach(it -> it.release());
        foundCornersList.clear();
        findBoardCornersPipe = null;
        broadcastState();
    }

    private void broadcastState() {
        logger.debug("RKT broadcastState");
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
                                settings.boardType,
                                settings.useMrCal));

        DataChangeService.getInstance()
                .publishEvent(OutgoingUIEvent.wrappedOf("calibrationData", state));
    }

    public boolean removeSnapshot(int index) {
        logger.debug("RKT removeSnapshot");
        try {
            foundCornersList.remove(index);
            return true;
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.error("Could not remove snapshot at index " + index, e);
            return false;
        }
    }

    public CameraCalibrationCoefficients cameraCalibrationCoefficients() {
        logger.debug("RKT cameraCalibrationCoefficients");
        return calibrationOutput.output;
    }
}
