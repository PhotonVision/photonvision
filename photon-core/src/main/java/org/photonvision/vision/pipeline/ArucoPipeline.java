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

import edu.wpi.first.apriltag.AprilTagPoseEstimate;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.util.Units;
import java.util.ArrayList;
import java.util.List;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.vision.aruco.ArucoDetectionResult;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.pipe.CVPipe.CVPipeResult;
import org.photonvision.vision.pipe.impl.*;
import org.photonvision.vision.pipe.impl.ArucoPoseEstimatorPipe.ArucoPoseEstimatorPipeParams;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.TrackedTarget;
import org.photonvision.vision.target.TrackedTarget.TargetCalculationParameters;

public class ArucoPipeline extends CVPipeline<CVPipelineResult, ArucoPipelineSettings> {
    private final ArucoDetectionPipe arucoDetectionPipe = new ArucoDetectionPipe();
    private final ArucoPoseEstimatorPipe poseEstimatorPipe = new ArucoPoseEstimatorPipe();
    private final CalculateFPSPipe calculateFPSPipe = new CalculateFPSPipe();

    public ArucoPipeline() {
        super(FrameThresholdType.GREYSCALE);
        settings = new ArucoPipelineSettings();
    }

    public ArucoPipeline(ArucoPipelineSettings settings) {
        super(FrameThresholdType.GREYSCALE);
        this.settings = settings;
    }

    @Override
    protected void setPipeParamsImpl() {
        var params = new ArucoDetectionPipeParams();
        // sanitize and record settings

        int threshMinSize = Math.max(3, settings.threshWinSizes.getFirst());
        settings.threshWinSizes.setFirst(threshMinSize);
        params.threshMinSize = threshMinSize;
        int threshStepSize = Math.max(2, settings.threshStepSize);
        settings.threshStepSize = threshStepSize;
        params.threshStepSize = threshStepSize;
        int threshMaxSize = Math.max(threshMinSize, settings.threshWinSizes.getSecond());
        settings.threshWinSizes.setSecond(threshMaxSize);
        params.threshMaxSize = threshMaxSize;
        params.threshConstant = settings.threshConstant;

        params.useCornerRefinement = settings.useCornerRefinement;
        params.refinementMaxIterations = settings.refineNumIterations;
        params.refinementMinErrorPx = settings.refineMinErrorPx;
        params.useAruco3 = settings.useAruco3;
        params.aruco3MinMarkerSideRatio = settings.aruco3MinMarkerSideRatio;
        params.aruco3MinCanonicalImgSide = settings.aruco3MinCanonicalImgSide;
        arucoDetectionPipe.setParams(params);

        if (frameStaticProperties.cameraCalibration != null) {
            var cameraMatrix = frameStaticProperties.cameraCalibration.getCameraIntrinsicsMat();
            if (cameraMatrix != null) {
                var estimatorParams =
                        new ArucoPoseEstimatorPipeParams(
                                frameStaticProperties.cameraCalibration, Units.inchesToMeters(6));
                poseEstimatorPipe.setParams(estimatorParams);
            }
        }
    }

    @Override
    protected CVPipelineResult process(Frame frame, ArucoPipelineSettings settings) {
        long sumPipeNanosElapsed = 0L;

        List<TrackedTarget> targetList;

        if (frame.type != FrameThresholdType.GREYSCALE) {
            // We asked for a GREYSCALE frame, but didn't get one -- best we can do is give up
            return new CVPipelineResult(0, 0, List.of());
        }

        CVPipeResult<List<ArucoDetectionResult>> tagDetectionPipeResult;
        tagDetectionPipeResult = arucoDetectionPipe.run(frame.processedImage);
        sumPipeNanosElapsed += tagDetectionPipeResult.nanosElapsed;

        // If we want to debug the thresholding steps, draw the first step to the color image
        if (settings.debugThreshold) {
            drawThresholdFrame(
                    frame.processedImage.getMat(),
                    frame.colorImage.getMat(),
                    settings.threshWinSizes.getFirst(),
                    settings.threshConstant);
        }

        targetList = new ArrayList<>();
        for (ArucoDetectionResult detection : tagDetectionPipeResult.output) {
            // TODO this should be in a pipe, not in the top level here (Matt)

            AprilTagPoseEstimate tagPoseEstimate = null;
            if (settings.solvePNPEnabled) {
                var poseResult = poseEstimatorPipe.run(detection);
                sumPipeNanosElapsed += poseResult.nanosElapsed;
                tagPoseEstimate = poseResult.output;
            }

            // populate the target list
            // Challenge here is that TrackedTarget functions with OpenCV Contour
            TrackedTarget target =
                    new TrackedTarget(
                            detection,
                            tagPoseEstimate,
                            new TargetCalculationParameters(
                                    false, null, null, null, null, frameStaticProperties));

            var correctedBestPose =
                    MathUtils.convertOpenCVtoPhotonTransform(target.getBestCameraToTarget3d());
            var correctedAltPose =
                    MathUtils.convertOpenCVtoPhotonTransform(target.getAltCameraToTarget3d());

            target.setBestCameraToTarget3d(
                    new Transform3d(correctedBestPose.getTranslation(), correctedBestPose.getRotation()));
            target.setAltCameraToTarget3d(
                    new Transform3d(correctedAltPose.getTranslation(), correctedAltPose.getRotation()));

            targetList.add(target);
        }

        var fpsResult = calculateFPSPipe.run(null);
        var fps = fpsResult.output;

        return new CVPipelineResult(sumPipeNanosElapsed, fps, targetList, frame);
    }

    private void drawThresholdFrame(Mat greyMat, Mat outputMat, int windowSize, double constant) {
        if (windowSize % 2 == 0) windowSize++;
        Imgproc.adaptiveThreshold(
                greyMat,
                outputMat,
                255,
                Imgproc.ADAPTIVE_THRESH_MEAN_C,
                Imgproc.THRESH_BINARY_INV,
                windowSize,
                constant);
    }
}
