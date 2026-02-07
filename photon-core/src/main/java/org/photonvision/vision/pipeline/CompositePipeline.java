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

import edu.wpi.first.apriltag.AprilTagDetection;
import edu.wpi.first.apriltag.AprilTagDetector;
import edu.wpi.first.apriltag.AprilTagPoseEstimate;
import edu.wpi.first.apriltag.AprilTagPoseEstimator.Config;
import edu.wpi.first.math.geometry.CoordinateSystem;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.util.Units;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.opencv.imgproc.Imgproc;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.configuration.NeuralNetworkModelManager;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.estimation.TargetModel;
import org.photonvision.targeting.MultiTargetPNPResult;
import org.photonvision.vision.apriltag.AprilTagFamily;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.objects.Model;
import org.photonvision.vision.objects.NullModel;
import org.photonvision.vision.opencv.DualOffsetValues;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipe.CVPipe.CVPipeResult;
import org.photonvision.vision.pipe.impl.*;
import org.photonvision.vision.pipe.impl.AprilTagDetectionPipe.AprilTagDetectionPipeParams;
import org.photonvision.vision.pipe.impl.AprilTagPoseEstimatorPipe.AprilTagPoseEstimatorPipeParams;
import org.photonvision.vision.pipe.impl.MultiTargetPNPPipe.MultiTargetPNPPipeParams;
import org.photonvision.vision.pipe.impl.ObjectDetectionPipe.ObjectDetectionPipeParams;
import org.photonvision.vision.pipeline.result.CompositePipelineResult;
import org.photonvision.vision.target.PotentialTarget;
import org.photonvision.vision.target.TargetOrientation;
import org.photonvision.vision.target.TrackedTarget;
import org.photonvision.vision.target.TrackedTarget.TargetCalculationParameters;

public class CompositePipeline extends CVPipeline<CompositePipelineResult, CompositePipelineSettings> {
    private static final FrameThresholdType PROCESSING_TYPE = FrameThresholdType.NONE;

    private final AprilTagDetectionPipe aprilTagDetectionPipe = new AprilTagDetectionPipe();
    private final AprilTagPoseEstimatorPipe singleTagPoseEstimatorPipe =
            new AprilTagPoseEstimatorPipe();
    private final MultiTargetPNPPipe multiTagPNPPipe = new MultiTargetPNPPipe();
    private final CalculateFPSPipe calculateFPSPipe = new CalculateFPSPipe();

    private final ObjectDetectionPipe objectDetectorPipe = new ObjectDetectionPipe();
    private final SortContoursPipe sortContoursPipe = new SortContoursPipe();
    private final Collect2dTargetsPipe collect2dTargetsPipe = new Collect2dTargetsPipe();
    private final FilterObjectDetectionsPipe filterContoursPipe = new FilterObjectDetectionsPipe();

    private final CVMat[] grayRing = new CVMat[] {new CVMat(), new CVMat()};
    private int grayRingIndex = 0;

    public CompositePipeline() {
        super(PROCESSING_TYPE);
        settings = new CompositePipelineSettings();
    }

    public CompositePipeline(CompositePipelineSettings settings) {
        super(PROCESSING_TYPE);
        this.settings = settings;
    }

    @Override
    protected void setPipeParamsImpl() {
        // AprilTag configuration
        settings.threads = Math.max(1, settings.threads);

        double tagWidth = Units.inchesToMeters(6.5);
        TargetModel tagModel = TargetModel.kAprilTag36h11;
        if (settings.tagFamily == AprilTagFamily.kTag16h5) {
            tagWidth = Units.inchesToMeters(6);
            tagModel = TargetModel.kAprilTag16h5;
        }

        var config = new AprilTagDetector.Config();
        config.numThreads = settings.threads;
        config.refineEdges = settings.refineEdges;
        config.quadSigma = (float) settings.blur;
        config.quadDecimate = settings.decimate;

        var quadParams = new AprilTagDetector.QuadThresholdParameters();
        quadParams.minClusterPixels = 5;
        quadParams.maxNumMaxima = 10;
        quadParams.criticalAngle = 45 * Math.PI / 180.0;
        quadParams.maxLineFitMSE = 10.0f;
        quadParams.minWhiteBlackDiff = 5;
        quadParams.deglitch = false;

        aprilTagDetectionPipe.setParams(
                new AprilTagDetectionPipeParams(settings.tagFamily, config, quadParams));

        if (frameStaticProperties.cameraCalibration != null) {
            var cameraMatrix = frameStaticProperties.cameraCalibration.getCameraIntrinsicsMat();
            if (cameraMatrix != null && cameraMatrix.rows() > 0) {
                var cx = cameraMatrix.get(0, 2)[0];
                var cy = cameraMatrix.get(1, 2)[0];
                var fx = cameraMatrix.get(0, 0)[0];
                var fy = cameraMatrix.get(1, 1)[0];

                singleTagPoseEstimatorPipe.setParams(
                        new AprilTagPoseEstimatorPipeParams(
                                new Config(tagWidth, fx, fy, cx, cy),
                                frameStaticProperties.cameraCalibration,
                                settings.numIterations));

                var atfl = ConfigManager.getInstance().getConfig().getApriltagFieldLayout();
                multiTagPNPPipe.setParams(
                        new MultiTargetPNPPipeParams(frameStaticProperties.cameraCalibration, atfl, tagModel));
            }
        }

        // Object detection configuration
        Optional<Model> selectedModel =
                settings.model != null
                        ? NeuralNetworkModelManager.getInstance()
                                .getModel(settings.model.modelPath().toString())
                        : Optional.empty();

        if (selectedModel.isEmpty()) {
            selectedModel = NeuralNetworkModelManager.getInstance().getDefaultModel();
        }

        if (selectedModel.isEmpty()) {
            selectedModel = Optional.of(NullModel.getInstance());
        }

        objectDetectorPipe.setParams(
                new ObjectDetectionPipeParams(settings.confidence, settings.nms, selectedModel.get()));

        DualOffsetValues dualOffsetValues =
                new DualOffsetValues(
                        settings.offsetDualPointA,
                        settings.offsetDualPointAArea,
                        settings.offsetDualPointB,
                        settings.offsetDualPointBArea);

        sortContoursPipe.setParams(
                new SortContoursPipe.SortContoursParams(
                        settings.contourSortMode,
                        settings.outputShowMultipleTargets ? MAX_MULTI_TARGET_RESULTS : 1,
                        frameStaticProperties));

        filterContoursPipe.setParams(
                new FilterObjectDetectionsPipe.FilterContoursParams(
                        settings.contourArea,
                        settings.contourRatio,
                        frameStaticProperties,
                        settings.contourTargetOrientation == TargetOrientation.Landscape));

        collect2dTargetsPipe.setParams(
                new Collect2dTargetsPipe.Collect2dTargetsParams(
                        settings.offsetRobotOffsetMode,
                        settings.offsetSinglePoint,
                        dualOffsetValues,
                        settings.contourTargetOffsetPointEdge,
                        settings.contourTargetOrientation,
                        frameStaticProperties));
    }

    @Override
    protected CompositePipelineResult process(Frame frame, CompositePipelineSettings settings) {
        long sumPipeNanosElapsed = 0L;

        var colorMat = frame.colorImage.getMat();
        if (!colorMat.empty()) {
            colorMat.copyTo(frame.processedImage.getMat());
        }

        List<TrackedTarget> aprilTagTargets = new ArrayList<>();
        Optional<MultiTargetPNPResult> multiTagResult = Optional.empty();

        if (settings.enableAprilTag && !colorMat.empty()) {
            CVMat grayMat = nextGrayMat(frame.colorImage);

            CVPipeResult<List<AprilTagDetection>> tagDetectionPipeResult =
                    aprilTagDetectionPipe.run(grayMat);
            sumPipeNanosElapsed += tagDetectionPipeResult.nanosElapsed;

            List<AprilTagDetection> detections = tagDetectionPipeResult.output;
            List<AprilTagDetection> usedDetections = new ArrayList<>();

            for (AprilTagDetection detection : detections) {
                if (detection.getDecisionMargin() < settings.decisionMargin) continue;
                if (detection.getHamming() > settings.hammingDist) continue;

                usedDetections.add(detection);

                TrackedTarget target =
                        new TrackedTarget(
                                detection,
                                null,
                                new TargetCalculationParameters(
                                        false, null, null, null, null, frameStaticProperties));
                aprilTagTargets.add(target);
            }

            if (settings.solvePNPEnabled && settings.doMultiTarget) {
                var multiTagOutput = multiTagPNPPipe.run(aprilTagTargets);
                sumPipeNanosElapsed += multiTagOutput.nanosElapsed;
                multiTagResult = multiTagOutput.output;
            }

            if (settings.solvePNPEnabled) {
                aprilTagTargets.clear();
                var atfl = ConfigManager.getInstance().getConfig().getApriltagFieldLayout();

                for (AprilTagDetection detection : usedDetections) {
                    AprilTagPoseEstimate tagPoseEstimate = null;
                    if (settings.doSingleTargetAlways
                            || !(multiTagResult.isPresent()
                                    && multiTagResult.get().fiducialIDsUsed.contains((short) detection.getId()))) {
                        var poseResult = singleTagPoseEstimatorPipe.run(detection);
                        sumPipeNanosElapsed += poseResult.nanosElapsed;
                        tagPoseEstimate = poseResult.output;
                    }

                    if (tagPoseEstimate == null && multiTagResult.isPresent()) {
                        var tagPose = atfl.getTagPose(detection.getId());
                        if (tagPose.isPresent()) {
                            var camToTag =
                                    new Transform3d(
                                            new Pose3d().plus(multiTagResult.get().estimatedPose.best),
                                            tagPose.get());
                            camToTag =
                                    CoordinateSystem.convert(
                                            camToTag, CoordinateSystem.NWU(), CoordinateSystem.EDN());
                            camToTag =
                                    new Transform3d(
                                            camToTag.getTranslation(),
                                            new Rotation3d(0, Math.PI, 0).plus(camToTag.getRotation()));
                            tagPoseEstimate = new AprilTagPoseEstimate(camToTag, camToTag, 0, 0);
                        }
                    }

                    TrackedTarget target =
                            new TrackedTarget(
                                    detection,
                                    tagPoseEstimate,
                                    new TargetCalculationParameters(
                                            false, null, null, null, null, frameStaticProperties));

                    if (tagPoseEstimate != null) {
                        var correctedBestPose =
                                MathUtils.convertOpenCVtoPhotonTransform(target.getBestCameraToTarget3d());
                        var correctedAltPose =
                                MathUtils.convertOpenCVtoPhotonTransform(target.getAltCameraToTarget3d());

                        target.setBestCameraToTarget3d(
                                new Transform3d(correctedBestPose.getTranslation(), correctedBestPose.getRotation()));
                        target.setAltCameraToTarget3d(
                                new Transform3d(correctedAltPose.getTranslation(), correctedAltPose.getRotation()));
                    }

                    aprilTagTargets.add(target);
                }
            }
        }

        List<TrackedTarget> objectTargets = new ArrayList<>();
        List<String> classNames = List.of();

        if (settings.enableObjectDetection && !colorMat.empty()) {
            CVPipeResult<List<NeuralNetworkPipeResult>> neuralNetworkResult =
                    objectDetectorPipe.run(frame.colorImage);
            sumPipeNanosElapsed += neuralNetworkResult.nanosElapsed;

            classNames = objectDetectorPipe.getClassNames();

            var filterContoursResult = filterContoursPipe.run(neuralNetworkResult.output);
            sumPipeNanosElapsed += filterContoursResult.nanosElapsed;

            CVPipeResult<List<PotentialTarget>> sortContoursResult =
                    sortContoursPipe.run(
                            filterContoursResult.output.stream()
                                    .map(shape -> new PotentialTarget(shape))
                                    .toList());
            sumPipeNanosElapsed += sortContoursResult.nanosElapsed;

            CVPipeResult<List<TrackedTarget>> collect2dTargetsResult =
                    collect2dTargetsPipe.run(sortContoursResult.output);
            sumPipeNanosElapsed += collect2dTargetsResult.nanosElapsed;

            objectTargets = collect2dTargetsResult.output;
        }

        var combinedTargets = new ArrayList<TrackedTarget>(aprilTagTargets.size() + objectTargets.size());
        combinedTargets.addAll(aprilTagTargets);
        combinedTargets.addAll(objectTargets);

        var fpsResult = calculateFPSPipe.run(null);
        var fps = fpsResult.output;

        return new CompositePipelineResult(
                frame.sequenceID,
                sumPipeNanosElapsed,
                fps,
                combinedTargets,
                multiTagResult,
                frame,
                classNames,
                aprilTagTargets,
                objectTargets);
    }

    private CVMat nextGrayMat(CVMat colorMat) {
        var grayMat = grayRing[grayRingIndex];
        grayRingIndex = (grayRingIndex + 1) % grayRing.length;
        Imgproc.cvtColor(colorMat.getMat(), grayMat.getMat(), Imgproc.COLOR_BGR2GRAY, 3);
        return grayMat;
    }

    @Override
    public void release() {
        aprilTagDetectionPipe.release();
        singleTagPoseEstimatorPipe.release();
        objectDetectorPipe.release();
        for (var cvMat : grayRing) {
            cvMat.release();
        }
        super.release();
    }
}
