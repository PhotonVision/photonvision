package org.photonvision.estimation;

import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform3d;

/** Holds various helper geometries describing the relation between camera and target. */
public class CameraTargetRelation {
    public final Pose3d camPose;
    public final Transform3d camToTarg;
    public final double camToTargDist;
    public final double camToTargDistXY;
    public final Rotation2d camToTargYaw;
    public final Rotation2d camToTargPitch;
    /** Angle from the camera's relative x-axis */
    public final Rotation2d camToTargAngle;

    public final Transform3d targToCam;
    public final Rotation2d targToCamYaw;
    public final Rotation2d targToCamPitch;
    /** Angle from the target's relative x-axis */
    public final Rotation2d targToCamAngle;

    public CameraTargetRelation(Pose3d cameraPose, Pose3d targetPose) {
        this.camPose = cameraPose;
        camToTarg = new Transform3d(cameraPose, targetPose);
        camToTargDist = camToTarg.getTranslation().getNorm();
        camToTargDistXY =
                Math.hypot(camToTarg.getTranslation().getX(), camToTarg.getTranslation().getY());
        camToTargYaw = new Rotation2d(camToTarg.getX(), camToTarg.getY());
        camToTargPitch = new Rotation2d(camToTargDistXY, -camToTarg.getZ());
        camToTargAngle =
                new Rotation2d(Math.hypot(camToTargYaw.getRadians(), camToTargPitch.getRadians()));
        targToCam = new Transform3d(targetPose, cameraPose);
        targToCamYaw = new Rotation2d(targToCam.getX(), targToCam.getY());
        targToCamPitch = new Rotation2d(camToTargDistXY, -targToCam.getZ());
        targToCamAngle =
                new Rotation2d(Math.hypot(targToCamYaw.getRadians(), targToCamPitch.getRadians()));
    }
}
