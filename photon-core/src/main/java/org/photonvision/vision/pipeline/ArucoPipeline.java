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
import edu.wpi.first.math.geometry.CoordinateSystem;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.util.Units;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.Objdetect;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.estimation.TargetModel;
import org.photonvision.targeting.MultiTargetPNPResult;
import org.photonvision.vision.aruco.ArucoDetectionResult;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.pipe.CVPipe.CVPipeResult;
import org.photonvision.vision.pipe.impl.*;
import org.photonvision.vision.pipe.impl.ArucoPoseEstimatorPipe.ArucoPoseEstimatorPipeParams;
import org.photonvision.vision.pipe.impl.MultiTargetPNPPipe.MultiTargetPNPPipeParams;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.TrackedTarget;
import org.photonvision.vision.target.TrackedTarget.TargetCalculationParameters;

public class ArucoPipeline extends CVPipeline<CVPipelineResult, ArucoPipelineSettings> {
    private ArucoDetectionPipe arucoDetectionPipe = new ArucoDetectionPipe();
    private ArucoPoseEstimatorPipe singleTagPoseEstimatorPipe = new ArucoPoseEstimatorPipe();
    private final MultiTargetPNPPipe multiTagPNPPipe = new MultiTargetPNPPipe();
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

        // for now, hard code tag width based on enum value
        // 2023/other: best guess is 6in
        double tagWidth = Units.inchesToMeters(6);
        TargetModel tagModel = TargetModel.kAprilTag16h5;
        switch (settings.tagFamily) {
            case kTag36h11:
                // 2024 tag, 6.5in
                params.tagFamily = Objdetect.DICT_APRILTAG_36h11;
                tagWidth = Units.inchesToMeters(6.5);
                tagModel = TargetModel.kAprilTag36h11;
                break;
            case kTag25h9:
                params.tagFamily = Objdetect.DICT_APRILTAG_25h9;
                break;
            default:
                params.tagFamily = Objdetect.DICT_APRILTAG_16h5;
        }

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
            if (cameraMatrix != null && cameraMatrix.rows() > 0) {
                var estimatorParams =
                        new ArucoPoseEstimatorPipeParams(frameStaticProperties.cameraCalibration, tagWidth);
                singleTagPoseEstimatorPipe.setParams(estimatorParams);

                // TODO global state ew
                var atfl = ConfigManager.getInstance().getConfig().getApriltagFieldLayout();
                multiTagPNPPipe.setParams(
                        new MultiTargetPNPPipeParams(frameStaticProperties.cameraCalibration, atfl, tagModel));
            }
        }
    }

    @Override
    protected CVPipelineResult process(Frame frame, ArucoPipelineSettings settings) {
        long sumPipeNanosElapsed = 0L;

        if (frame.type != FrameThresholdType.GREYSCALE) {
            // We asked for a GREYSCALE frame, but didn't get one -- best we can do is give up
            return new CVPipelineResult(frame.sequenceID, 0, 0, List.of(), frame);
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

        List<TrackedTarget> targetList = new ArrayList<>();
        for (ArucoDetectionResult detection : tagDetectionPipeResult.output) {
            // Populate target list for multitag
            // (TODO: Address circular dependencies. Multitag only requires corners and IDs, this should
            // not be necessary.)
            TrackedTarget target =
                    new TrackedTarget(
                            detection,
                            null,
                            new TargetCalculationParameters(
                                    false, null, null, null, null, frameStaticProperties));

            targetList.add(target);
        }

        // Do multi-tag pose estimation
        Optional<MultiTargetPNPResult> multiTagResult = Optional.empty();
        if (settings.solvePNPEnabled && settings.doMultiTarget) {
            var multiTagOutput = multiTagPNPPipe.run(targetList);
            sumPipeNanosElapsed += multiTagOutput.nanosElapsed;
            multiTagResult = multiTagOutput.output;
        }

        // Do single-tag pose estimation
        if (settings.solvePNPEnabled) {
            // Clear target list that was used for multitag so we can add target transforms
            targetList.clear();
            // TODO global state again ew
            var atfl = ConfigManager.getInstance().getConfig().getApriltagFieldLayout();

            for (ArucoDetectionResult detection : tagDetectionPipeResult.output) {
                AprilTagPoseEstimate tagPoseEstimate = null;
                // Do single-tag estimation when "always enabled" or if a tag was not used for multitag
                if (settings.doSingleTargetAlways
                        || !(multiTagResult.isPresent()
                                && multiTagResult.get().fiducialIDsUsed.contains((short) detection.getId()))) {
                    var poseResult = singleTagPoseEstimatorPipe.run(detection);
                    sumPipeNanosElapsed += poseResult.nanosElapsed;
                    tagPoseEstimate = poseResult.output;
                }

                // If single-tag estimation was not done, this is a multi-target tag from the layout
                if (tagPoseEstimate == null && multiTagResult.isPresent()) {
                    // compute this tag's camera-to-tag transform using the multitag result
                    var tagPose = atfl.getTagPose(detection.getId());
                    if (tagPose.isPresent()) {
                        var camToTag =
                                new Transform3d(
                                        new Pose3d().plus(multiTagResult.get().estimatedPose.best), tagPose.get());
                        // match expected OpenCV coordinate system
                        camToTag =
                                CoordinateSystem.convert(camToTag, CoordinateSystem.NWU(), CoordinateSystem.EDN());

                        tagPoseEstimate = new AprilTagPoseEstimate(camToTag, camToTag, 0, 0);
                    }
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
        }

        var fpsResult = calculateFPSPipe.run(null);
        var fps = fpsResult.output;

        return new CVPipelineResult(
                frame.sequenceID, sumPipeNanosElapsed, fps, targetList, multiTagResult, frame);
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

    @Override
    public void release() {
        arucoDetectionPipe.release();
        singleTagPoseEstimatorPipe.release();
        arucoDetectionPipe = null;
        singleTagPoseEstimatorPipe = null;
        super.release();
    }
}
