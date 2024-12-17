package org.photonvision.vision.pipeline;

import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.apriltag.AprilTagFamily;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.frame.provider.FileFrameProvider;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.TargetModel;
import org.photonvision.vision.target.TrackedTarget;

public class UncropApriltagTest {

    @BeforeEach
    public void setup() {
        TestUtils.loadLibraries();
        ConfigManager.getInstance().load();
    }

    @Test
    public void testApriltagFacingCamera() {
        var pipeline = new AprilTagPipeline();

        pipeline.getSettings().inputShouldShow = true;
        pipeline.getSettings().outputShouldDraw = true;
        pipeline.getSettings().solvePNPEnabled = true;
        pipeline.getSettings().cornerDetectionAccuracyPercentage = 4;
        pipeline.getSettings().cornerDetectionUseConvexHulls = true;
        pipeline.getSettings().targetModel = TargetModel.kAprilTag6p5in_36h11;
        pipeline.getSettings().tagFamily = AprilTagFamily.kTag36h11;

        var frameProvider =
                new FileFrameProvider(
                        TestUtils.getApriltagImagePath(TestUtils.ApriltagTestImages.kTag1_640_480, false),
                        TestUtils.WPI2020Image.FOV,
                        TestUtils.get2020LifeCamCoeffs(false));
        frameProvider.requestFrameThresholdType(pipeline.getThresholdType());

        CVPipelineResult pipelineResult;
        pipelineResult = pipeline.run(frameProvider.get(), QuirkyCamera.DefaultCamera);

        printTestResults(pipelineResult);

        // Draw on input
        var outputPipe = new OutputStreamPipeline();
        var ret =
                outputPipe.process(
                        pipelineResult.inputAndOutputFrame, pipeline.getSettings(), pipelineResult.targets);

        TestUtils.showImage(ret.inputAndOutputFrame.processedImage.getMat(), "Pipeline output", 999999);

        pipeline.getSettings().static_x = 100;

        CVPipelineResult croppedResults;
        croppedResults = pipeline.run(frameProvider.get(), QuirkyCamera.DefaultCamera);

        printTestResults(croppedResults);

        var ret_cropped =
                outputPipe.process(
                        croppedResults.inputAndOutputFrame, pipeline.getSettings(), croppedResults.targets);

        TestUtils.showImage(
                ret_cropped.inputAndOutputFrame.processedImage.getMat(), "Cropped Pipeline output", 999999);
        // these numbers are not *accurate*, but they are known and expected
        var target = pipelineResult.targets.get(0);
        var croppedTarget = croppedResults.targets.get(0);

        // Test corner order
        var corners = target.getTargetCorners();
        var croppedCorners = croppedTarget.getTargetCorners();
        double acceptedDelta = 0.005;

        Assertions.assertEquals(corners.get(0).x, croppedCorners.get(0).x, acceptedDelta);
        Assertions.assertEquals(corners.get(0).y, croppedCorners.get(0).y, acceptedDelta);
        Assertions.assertEquals(corners.get(1).x, croppedCorners.get(1).x, acceptedDelta);
        Assertions.assertEquals(corners.get(1).y, croppedCorners.get(1).y, acceptedDelta);
        Assertions.assertEquals(corners.get(2).x, croppedCorners.get(2).x, acceptedDelta);
        Assertions.assertEquals(corners.get(2).y, croppedCorners.get(2).y, acceptedDelta);
        Assertions.assertEquals(corners.get(3).x, croppedCorners.get(3).x, acceptedDelta);
        Assertions.assertEquals(corners.get(3).y, croppedCorners.get(3).y, acceptedDelta);

        var pose = target.getBestCameraToTarget3d();
        var croppedPose = croppedTarget.getBestCameraToTarget3d();
        // Test pose estimate translation and rotation
        Assertions.assertEquals(
                pose.getTranslation().getX(), croppedPose.getTranslation().getX(), acceptedDelta);
        Assertions.assertEquals(
                pose.getTranslation().getY(), croppedPose.getTranslation().getY(), acceptedDelta);
        Assertions.assertEquals(
                pose.getTranslation().getZ(), croppedPose.getTranslation().getZ(), acceptedDelta);
        Assertions.assertEquals(
                pose.getRotation().getX(), croppedPose.getRotation().getX(), acceptedDelta);
        Assertions.assertEquals(
                pose.getRotation().getY(), croppedPose.getRotation().getY(), acceptedDelta);
        Assertions.assertEquals(
                pose.getRotation().getZ(), croppedPose.getRotation().getZ(), acceptedDelta);
    }

    private static void printTestResults(CVPipelineResult pipelineResult) {
        double fps = 1000 / pipelineResult.getLatencyMillis();
        System.out.println(
                "Pipeline ran in " + pipelineResult.getLatencyMillis() + "ms (" + fps + " " + "fps)");
        System.out.println("Found " + pipelineResult.targets.size() + " valid targets");
        System.out.println(
                "Found targets at "
                        + pipelineResult.targets.stream()
                                .map(TrackedTarget::getBestCameraToTarget3d)
                                .collect(Collectors.toList()));
    }
}
