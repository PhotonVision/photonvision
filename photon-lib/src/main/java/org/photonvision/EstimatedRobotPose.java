package org.photonvision;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.photonvision.targeting.PhotonPipelineResult;

import edu.wpi.first.math.geometry.Pose3d;

public class EstimatedRobotPose {
    Pose3d estimatedPose;
    List<CameraPipelineResult> cameraPipelineResults;

    public EstimatedRobotPose(Pose3d estimatedPose, List<CameraPipelineResult> cameraPipelineResults) {
        this.estimatedPose = estimatedPose;
        this.cameraPipelineResults = cameraPipelineResults;
    }

    public EstimatedRobotPose(Pose3d estimatedPose, CameraPipelineResult cameraPipelineResult) {
        this.estimatedPose = estimatedPose;
        this.cameraPipelineResults = List.of(cameraPipelineResult);
    }

    public EstimatedRobotPose(Pose3d estimatedPose, PhotonCamera camera, PhotonPipelineResult photonPipelineResult) {
        this.estimatedPose = estimatedPose;
        this.cameraPipelineResults = List.of(new CameraPipelineResult(camera, photonPipelineResult));
    }

    public double getTimestamp() {
        return cameraPipelineResults
                .stream()
                .flatMap(r -> r.photonPipelineResults.stream())
                .collect(Collectors.averagingDouble(cpr -> cpr.getTimestampSeconds()));
    }

    public static class CameraPipelineResult {
        PhotonCamera camera;
        List<PhotonPipelineResult> photonPipelineResults;

        public CameraPipelineResult(PhotonCamera camera, List<PhotonPipelineResult> photonPipelineResults) {
            this.camera = camera;
            this.photonPipelineResults = photonPipelineResults;
        }
        public CameraPipelineResult(PhotonCamera camera, PhotonPipelineResult photonPipelineResult) {
            this.camera = camera;
            this.photonPipelineResults = List.of(photonPipelineResult);
        }
        
    }
}
