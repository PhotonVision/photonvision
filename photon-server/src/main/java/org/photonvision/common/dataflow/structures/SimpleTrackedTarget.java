package org.photonvision.common.dataflow.structures;

import edu.wpi.first.wpilibj.geometry.Pose2d;
import edu.wpi.first.wpilibj.geometry.Rotation2d;
import org.photonvision.vision.target.TrackedTarget;

import java.util.Objects;

public class SimpleTrackedTarget extends BytePackable {
    public static final int PACK_SIZE_BYTES = Double.BYTES * 6;

    private double yaw;
    private double pitch;
    private double area;
    private Pose2d robotRelativePose = new Pose2d();

    public SimpleTrackedTarget() {}

    public SimpleTrackedTarget(double yaw, double pitch, double area, Pose2d pose) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.area = area;
        robotRelativePose = pose;
    }

    public SimpleTrackedTarget(TrackedTarget origTarget) {
        yaw = origTarget.getYaw();
        pitch = origTarget.getPitch();
        area = origTarget.getArea();
        robotRelativePose = origTarget.getRobotRelativePose();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleTrackedTarget that = (SimpleTrackedTarget) o;
        return Double.compare(that.yaw, yaw) == 0
                && Double.compare(that.pitch, pitch) == 0
                && Double.compare(that.area, area) == 0
                && Objects.equals(robotRelativePose, that.robotRelativePose);
    }

    @Override
    public int hashCode() {
        return Objects.hash(yaw, pitch, area, robotRelativePose);
    }

    @Override
    public byte[] toByteArray() {
        resetBufferPosition();
        byte[] data = new byte[PACK_SIZE_BYTES];

        bufferData(yaw, data);
        bufferData(pitch, data);
        bufferData(area, data);
        bufferData(robotRelativePose.getTranslation().getX(), data);
        bufferData(robotRelativePose.getTranslation().getY(), data);
        bufferData(robotRelativePose.getRotation().getDegrees(), data);

        return data;
    }

    @Override
    public void fromByteArray(byte[] src) {
        resetBufferPosition();
        if (src.length < PACK_SIZE_BYTES) {
            // TODO: log error?
            return;
        }

        yaw = unbufferDouble(src);
        pitch = unbufferDouble(src);
        area = unbufferDouble(src);

        var poseX = unbufferDouble(src);
        var poseY = unbufferDouble(src);
        var poseR = unbufferDouble(src);
        robotRelativePose = new Pose2d(poseX, poseY, Rotation2d.fromDegrees(poseR));
    }
}
