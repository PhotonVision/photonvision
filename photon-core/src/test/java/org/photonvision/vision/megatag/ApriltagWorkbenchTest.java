package org.photonvision.vision.megatag;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.photonvision.common.util.TestUtils;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.TargetCorner;
import org.photonvision.vision.estimation.CameraProperties;
import org.photonvision.vision.estimation.VisionEstimation;

import edu.wpi.first.apriltag.AprilTag;
import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.apriltag.AprilTagFields;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class ApriltagWorkbenchTest {
    @Test
    public void testMeme() throws IOException, InterruptedException {
        TestUtils.loadLibraries();

        NetworkTableInstance instance = NetworkTableInstance.getDefault();
        instance.stopServer();
        // set the NT server if simulating this code.
        // "localhost" for photon on desktop, or "photonvision.local" / "[ip-address]"
        // for coprocessor
        instance.setServer("localhost");
        instance.startClient4("myRobot");

        var robotToCamera = new Transform3d();
        var props = CameraProperties.LIFECAM_1280_720p;
        var cam = new PhotonCamera("WPI2023");
        var tagLayout = AprilTagFieldLayout.loadFromResource(AprilTagFields.k2023ChargedUp.m_resourceFile);

        while (!Thread.interrupted()) {
            Thread.sleep(500);
            var visCorners = new ArrayList<TargetCorner>();
            var knownVisTags = new ArrayList<AprilTag>();
            var fieldToCams = new ArrayList<Pose3d>();
            var result = cam.getLatestResult();
            System.out.println(result.getTargets().size());

            for (var target : result.getTargets()) {
                visCorners.addAll(target.getDetectedCorners());
                Pose3d tagPose = tagLayout.getTagPose(target.getFiducialId()).get();
                // actual layout poses of visible tags
                knownVisTags.add(new AprilTag(target.getFiducialId(), tagPose));

                fieldToCams.add(tagPose.transformBy(target.getBestCameraToTarget().inverse()));
            }

            final var data = new ArrayList<Double>();
            fieldToCams.stream().forEach(it -> data.addAll(List.of(
                    it.getX(),
                    it.getY(),
                    it.getZ(),
                    it.getRotation().getQuaternion().getW(),
                    it.getRotation().getQuaternion().getX(),
                    it.getRotation().getQuaternion().getY(),
                    it.getRotation().getQuaternion().getZ())));
            SmartDashboard.putNumberArray("fieldToCams", data.toArray(new Double[] {}));

            // data = new ArrayList<Double>();o
            data.clear();
            knownVisTags.stream().forEach(it -> data.addAll(List.of(
                    it.pose.getX(),
                    it.pose.getY(),
                    it.pose.getZ(),
                    it.pose.getRotation().getQuaternion().getW(),
                    it.pose.getRotation().getQuaternion().getX(),
                    it.pose.getRotation().getQuaternion().getY(),
                    it.pose.getRotation().getQuaternion().getZ())));
            SmartDashboard.putNumberArray("seenTags", data.toArray(new Double[] {}));

            // multi-target solvePNP
            if (result.getTargets().size() > 1) {
                data.clear();
                visCorners.stream().forEach(it -> data.addAll(List.of(
                        it.x)));
                SmartDashboard.putNumberArray("cornersX", data.toArray(new Double[] {}));
                data.clear();
                visCorners.stream().forEach(it -> data.addAll(List.of(
                        it.y)));
                SmartDashboard.putNumberArray("cornersY", data.toArray(new Double[] {}));

                var pnpResults = VisionEstimation.estimateCamPosePNP(
                        props,
                        visCorners,
                        knownVisTags);
                var best = new Pose3d()
                        .plus(pnpResults.best) // field-to-camera
                        .plus(robotToCamera.inverse()); // field-to-robot
                // var alt = new Pose3d()
                // .plus(pnpResults.alt) // field-to-camera
                // .plus(robotToCamera.inverse()); // field-to-robot

                SmartDashboard.putNumberArray("multiTagPNP", new double[] {
                        best.getX(),
                        best.getY(),
                        best.getZ(),
                        best.getRotation().getQuaternion().getW(),
                        best.getRotation().getQuaternion().getX(),
                        best.getRotation().getQuaternion().getY(),
                        best.getRotation().getQuaternion().getZ()
                });
            }
        }
    }
}
