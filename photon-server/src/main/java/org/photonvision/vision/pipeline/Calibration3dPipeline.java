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

import org.opencv.core.Mat;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.pipe.CVPipeResult;
import org.photonvision.vision.pipe.impl.Calibrate3dPipe;
import org.photonvision.vision.pipe.impl.FindBoardCornersPipe;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.processes.PipelineManager;

import java.util.ArrayList;
import java.util.List;

public class Calibration3dPipeline extends CVPipeline<CVPipelineResult, Calibration3dPipelineSettings> {

    private final FindBoardCornersPipe findBoardCornersPipe = new FindBoardCornersPipe();
    private final Calibrate3dPipe calibrate3dPipe = new Calibrate3dPipe();

    private int numSnapshots = 0;
    private boolean calibrate = false;
    private boolean takeSnapshot = false;

    private List<Mat> boardSnapshots = new ArrayList<>();
    private CVPipeResult<List<List<Mat>>> findCornersPipeOutput;
    private CVPipeResult<CameraCalibrationCoefficients> calibrationOutput;

    public Calibration3dPipeline() {
        settings = new CVPipelineSettings();
        settings.pipelineIndex = PipelineManager.CAL_3D_INDEX;
    }


    @Override
    protected void setPipeParams(FrameStaticProperties frameStaticProperties, Calibration3dPipelineSettings settings) {
        FindBoardCornersPipe.FindCornersPipeParams findCornersPipeParams = new FindBoardCornersPipe.FindCornersPipeParams(settings.boardHeight, settings.boardWidth, settings.isUsingChessboard, settings.gridSize);
        findBoardCornersPipe.setParams(findCornersPipeParams);

        Calibrate3dPipe.CalibratePipeParams calibratePipeParams = new Calibrate3dPipe.CalibratePipeParams(settings.resolution);
        calibrate3dPipe.setParams(calibratePipeParams);
    }

    @Override
    protected CVPipelineResult process(Frame frame, Calibration3dPipelineSettings settings) {
        setPipeParams(frame.frameStaticProperties, settings);

        long sumPipeNanosElapsed = 0L;

        if(hasEnough() && calibrate) {

            findCornersPipeOutput = findBoardCornersPipe.apply(boardSnapshots);
            sumPipeNanosElapsed += findCornersPipeOutput.nanosElapsed;

            calibrationOutput = calibrate3dPipe.apply(findCornersPipeOutput.result);
            sumPipeNanosElapsed += calibrationOutput.nanosElapsed;

            calibrate = false;
            numSnapshots = 0;
            boardSnapshots.clear();
        } else if(findBoardCornersPipe.findBoardCorners(frame.image.getMat()) && takeSnapshot){

            //See if mat is empty
            System.out.println(frame.image.getMat().empty()); //Prints False
            boardSnapshots.add(frame.image.getMat());
            System.out.println(boardSnapshots.get(0).empty()); //Prints True

            takeSnapshot = false;
            numSnapshots++;
        }

        return new CVPipelineResult(
                MathUtils.nanosToMillis(sumPipeNanosElapsed),
                null,
                frame);
    }

    public boolean hasEnough(){
        return numSnapshots >= 25;
    }

    public void startCalibration(){
        calibrate = true;
    }


    public void takeSnapshot(){
        takeSnapshot = true;
    }

    public double[] perViewErrors(){
        return calibrationOutput.result.perViewErrors;
    }

    public CameraCalibrationCoefficients cameraCalibrationCoefficients(){
        return calibrationOutput.result;
    }

}