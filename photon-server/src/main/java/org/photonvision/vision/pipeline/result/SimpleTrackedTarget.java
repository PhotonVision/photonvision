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

package org.photonvision.vision.pipeline.result;

import edu.wpi.first.wpilibj.geometry.Pose2d;
import edu.wpi.first.wpilibj.geometry.Rotation2d;
import java.util.Objects;
import org.photonvision.vision.target.TrackedTarget;

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

    public SimpleTrackedTarget(TrackedTarget t) {
        this(t.getYaw(), t.getPitch(), t.getArea(), t.getRobotRelativePose());
    }

    public double getYaw() {
        return yaw;
    }

    public double getPitch() {
        return pitch;
    }

    public double getArea() {
        return area;
    }

    public Pose2d getRobotRelativePose() {
        return robotRelativePose;
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
        if (robotRelativePose != null) {
            bufferData(robotRelativePose.getTranslation().getX(), data);
            bufferData(robotRelativePose.getTranslation().getY(), data);
            bufferData(robotRelativePose.getRotation().getDegrees(), data);
        } else {
            bufferData((double) 0, data);
            bufferData((double) 0, data);
            bufferData((double) 0, data);
        }

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
