/*
 * Copyright (C) 2020 Photon Vision.
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

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.wpi.first.wpilibj.util.Units;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.opencv.core.Mat;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.SerializationUtils;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.server.SocketHandler;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipe.CVPipe.CVPipeResult;
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

    // Getter methods have been set for calibrate and takeSnapshot
    private int numSnapshots = 0;
    private boolean calibrate = false;
    private boolean takeSnapshot = false;

    // BoardSnapshots is a list of all valid snapshots taken
    private ArrayList<Mat> boardSnapshots;

    // Output of the corners
    private CVPipeResult<List<List<Mat>>> findCornersPipeOutput;

    /// Output of the calibration, getter method is set for this.
    private CVPipeResult<CameraCalibrationCoefficients> calibrationOutput;

    private static final int kMinSnapshots = 25;

    public Calibrate3dPipeline() {
        this.settings = new Calibration3dPipelineSettings();
        this.boardSnapshots = new ArrayList<>();
    }

    @Override
    protected void setPipeParams(
            FrameStaticProperties frameStaticProperties, Calibration3dPipelineSettings settings) {
        FindBoardCornersPipe.FindCornersPipeParams findCornersPipeParams =
                new FindBoardCornersPipe.FindCornersPipeParams(
                        settings.boardHeight, settings.boardWidth, settings.boardType, settings.gridSize);
        findBoardCornersPipe.setParams(findCornersPipeParams);

        Calibrate3dPipe.CalibratePipeParams calibratePipeParams =
                new Calibrate3dPipe.CalibratePipeParams(settings.resolution);
        calibrate3dPipe.setParams(calibratePipeParams);
    }

    @Override
    protected CVPipelineResult process(Frame frame, Calibration3dPipelineSettings settings) {
        // Set the pipe parameters
        setPipeParams(frame.frameStaticProperties, settings);

        long sumPipeNanosElapsed = 0L;

        // Check if the frame has chessboard corners
        var hasBoard = findBoardCornersPipe.findBoardCorners(frame.image.getMat());

        // hasEnough() is a getter method for numSnapshots that checks if there are more than 25
        // snapshots
        // calibrate will be true when it is get by it's putter method
        if (hasEnough() && calibrate) {

            calibrate = false;
        } else if (takeSnapshot) {
            if (hasBoard.getLeft()) {
                Mat board = new Mat();
                frame.image.getMat().copyTo(board);
                // Add board to snapshots
                boardSnapshots.add(board);

                // Set snapshot to false and increment number of snapshots taken
                takeSnapshot = false;
                numSnapshots++;

                // update the UI
                try {
                    var state =
                            SerializationUtils.objectToHashMap(
                                    new UICalibrationData(
                                            settings.cameraVideoModeIndex,
                                            numSnapshots,
                                            kMinSnapshots,
                                            hasEnough(),
                                            Units.metersToInches(settings.gridSize),
                                            settings.boardWidth,
                                            settings.boardHeight,
                                            settings.boardType));
                    var map = new SocketHandler.UIMap();
                    map.put("calibrationData", state);
                    SocketHandler.getInstance().broadcastMessage(map, null);
                } catch (JsonProcessingException e) {
                    logger.error(Arrays.toString(e.getStackTrace()));
                }

                return new CVPipelineResult(
                        MathUtils.nanosToMillis(sumPipeNanosElapsed),
                        Collections.emptyList(),
                        new Frame(new CVMat(hasBoard.getRight()), frame.frameStaticProperties));
            }
        }

        // Return the drawn chessboard if corners are found, if not, then return the input image.
        return new CVPipelineResult(
                MathUtils.nanosToMillis(sumPipeNanosElapsed),
                null,
                new Frame(
                        new CVMat(hasBoard.getLeft() ? hasBoard.getRight() : frame.image.getMat()),
                        frame.frameStaticProperties));
    }

    public boolean hasEnough() {
        return numSnapshots >= kMinSnapshots;
    }

    public CameraCalibrationCoefficients tryCalibration() {
        if (!hasEnough()) return null;

        /*Pass the board corners to the pipe, which will check again to see if all boards are valid
        and returns the corresponding image and object points*/
        findCornersPipeOutput = findBoardCornersPipe.run(boardSnapshots);
        // Increment the time it took to process all board pics to total elapsed time

        calibrationOutput = calibrate3dPipe.run(findCornersPipeOutput.output);

        return calibrationOutput.output;
    }

    public void startCalibration() {
        calibrate = true;
    }

    public void takeSnapshot() {
        takeSnapshot = true;
    }

    public double[] perViewErrors() {
        return calibrationOutput.output.perViewErrors;
    }

    public void finishCalibration() {
        numSnapshots = 0;
        boardSnapshots.clear();
    }

    public boolean removeSnapshot(int index) {
        try {
            boardSnapshots.remove(index);
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
