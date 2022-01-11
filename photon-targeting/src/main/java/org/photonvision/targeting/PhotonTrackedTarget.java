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
package org.photonvision.targeting;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.photonvision.common.dataflow.structures.Packet;

public class PhotonTrackedTarget {
    public static final int PACK_SIZE_BYTES = Double.BYTES * (7 + 2 * 4);

    private double yaw;
    private double pitch;
    private double area;
    private double skew;
    private Transform2d cameraToTarget = new Transform2d();
    private List<TargetCorner> targetCorners;

    public PhotonTrackedTarget() {}

    /** Construct a tracked target, given exactly 4 corners */
    public PhotonTrackedTarget(
            double yaw,
            double pitch,
            double area,
            double skew,
            Transform2d pose,
            List<TargetCorner> corners) {
        assert corners.size() == 4;
        this.yaw = yaw;
        this.pitch = pitch;
        this.area = area;
        this.skew = skew;
        this.cameraToTarget = pose;
        this.targetCorners = corners;
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

    public double getSkew() {
        return skew;
    }

    /**
     * Return a list of the 4 corners in image space (origin top left, x left, y down), in no
     * particular order, of the minimum area bounding rectangle of this target
     */
    public List<TargetCorner> getCorners() {
        return targetCorners;
    }

    public Transform2d getCameraToTarget() {
        return cameraToTarget;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhotonTrackedTarget that = (PhotonTrackedTarget) o;
        return Double.compare(that.yaw, yaw) == 0
                && Double.compare(that.pitch, pitch) == 0
                && Double.compare(that.area, area) == 0
                && Objects.equals(cameraToTarget, that.cameraToTarget)
                && Objects.equals(targetCorners, that.targetCorners);
    }

    @Override
    public int hashCode() {
        return Objects.hash(yaw, pitch, area, cameraToTarget);
    }

    /**
     * Populates the fields of this class with information from the incoming packet.
     *
     * @param packet The incoming packet.
     * @return The incoming packet.
     */
    public Packet createFromPacket(Packet packet) {
        this.yaw = packet.decodeDouble();
        this.pitch = packet.decodeDouble();
        this.area = packet.decodeDouble();
        this.skew = packet.decodeDouble();

        double x = packet.decodeDouble();
        double y = packet.decodeDouble();
        double r = packet.decodeDouble();

        this.targetCorners = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            double cx = packet.decodeDouble();
            double cy = packet.decodeDouble();
            targetCorners.add(new TargetCorner(cx, cy));
        }

        this.cameraToTarget = new Transform2d(new Translation2d(x, y), Rotation2d.fromDegrees(r));

        return packet;
    }

    /**
     * Populates the outgoing packet with information from the current target.
     *
     * @param packet The outgoing packet.
     * @return The outgoing packet.
     */
    public Packet populatePacket(Packet packet) {
        packet.encode(yaw);
        packet.encode(pitch);
        packet.encode(area);
        packet.encode(skew);
        packet.encode(cameraToTarget.getTranslation().getX());
        packet.encode(cameraToTarget.getTranslation().getY());
        packet.encode(cameraToTarget.getRotation().getDegrees());

        for (int i = 0; i < 4; i++) {
            packet.encode(targetCorners.get(i).x);
            packet.encode(targetCorners.get(i).y);
        }

        return packet;
    }
}
