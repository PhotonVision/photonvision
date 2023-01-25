package org.photonvision.vision.estimation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;

/**
 * Describes the shape of the target
 */
public class TargetModel {
    /**
     * Translations of this target's corners relative to its pose
     */
    public final List<Translation3d> cornerOffsets;
    public final boolean isPlanar;
    public final double widthMeters;
    public final double heightMeters;
    public final double areaSqMeters;

    public TargetModel(List<Translation3d> cornerOffsets, double widthMeters, double heightMeters) {
        if(cornerOffsets == null || cornerOffsets.size() <= 2) {
            cornerOffsets = new ArrayList<>();
            this.isPlanar = false;
        }
        else {
            boolean cornersPlanar = true;
            for(Translation3d corner : cornerOffsets) {
                if(corner.getX() != 0) cornersPlanar = false;
            }
            if(cornerOffsets.size() != 4 || !cornersPlanar) {
                throw new IllegalArgumentException(
                    String.format(
                        "Supplied target corners (%s) must total 4 and be planar (all X == 0).",
                        cornerOffsets.size()
                    )
                );
            };
            this.isPlanar = true;
        }
        this.cornerOffsets = cornerOffsets;
        this.widthMeters = widthMeters;
        this.heightMeters = heightMeters;
        this.areaSqMeters = widthMeters * heightMeters;
    }
    /**
     * Creates a rectangular, planar target model given the width and height.
     */
    public static TargetModel ofPlanarRect(double widthMeters, double heightMeters) {
        // 4 corners of rect with its pose as origin
        return new TargetModel(
            List.of(
                // this order is relevant for solvePNP
                new Translation3d(0, -widthMeters/2.0, heightMeters/2.0),
                new Translation3d(0, widthMeters/2.0, heightMeters/2.0),
                new Translation3d(0, widthMeters/2.0, -heightMeters/2.0),
                new Translation3d(0, -widthMeters/2.0, -heightMeters/2.0)
            ),
            widthMeters, heightMeters
        );
    }
    /**
     * Creates a spherical target which has similar dimensions when viewed from any angle.
     */
    public static TargetModel ofSphere(double diameterMeters) {
        // to get area = PI*r^2
        double assocSideLengths = Math.sqrt(Math.PI)*(diameterMeters/2.0);
        return new TargetModel(null, assocSideLengths, assocSideLengths);
    }

    /**
     * This target's corners offset from its field pose.
     */
    public List<Translation3d> getFieldCorners(Pose3d targetPose) {
        return cornerOffsets.stream()
            .map(t -> targetPose.plus(new Transform3d(t, new Rotation3d())).getTranslation())
            .collect(Collectors.toList());
    }

    /**
     * This target's corners offset from its field pose, which is facing the camera.
     */
    public List<Translation3d> getAgnosticFieldCorners(Pose3d cameraPose, Pose3d targetPose) {
        var rel = new CameraTargetRelation(cameraPose, targetPose);
        // this target's pose but facing the camera pose
        var facingPose = new Pose3d(
            targetPose.getTranslation(),
            new Rotation3d(0, rel.camToTargPitch.getRadians(), rel.camToTargYaw.getRadians())
        );
        // find field corners based on this model's width/height if it was facing the camera
        return TargetModel.ofPlanarRect(widthMeters, heightMeters)
            .getFieldCorners(facingPose);
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(obj instanceof TargetModel) {
            var o = (TargetModel)obj;
            return cornerOffsets.equals(o.cornerOffsets) &&
                    widthMeters == o.widthMeters &&
                    heightMeters == o.heightMeters &&
                    areaSqMeters == o.areaSqMeters;
        }
        return false;
    }
}
