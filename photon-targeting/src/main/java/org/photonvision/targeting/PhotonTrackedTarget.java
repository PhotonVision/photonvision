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

import edu.wpi.first.math.geometry.Quaternion;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.photonvision.common.dataflow.structures.Packet;

public class PhotonTrackedTarget {
    public static final int PACK_SIZE_BYTES = Double.BYTES * (5 + 7 + 2 * 4 + 1 + 7);

    private double yaw;
    private double pitch;
    private double area;
    private double skew;
    private int fiducialId;
    private Transform3d bestCameraToTarget = new Transform3d();
    private Transform3d altCameraToTarget = new Transform3d();
    private double poseAmbiguity;
    private List<TargetCorner> targetCorners;

    public PhotonTrackedTarget() {}

    /** Construct a tracked target, given exactly 4 corners */
    public PhotonTrackedTarget(
            double yaw,
            double pitch,
            double area,
            double skew,
            int id,
            Transform3d pose,
            Transform3d altPose,
            double ambiguity,
            List<TargetCorner> corners) {
        assert corners.size() == 4;
        this.yaw = yaw;
        this.pitch = pitch;
        this.area = area;
        this.skew = skew;
        this.fiducialId = id;
        this.bestCameraToTarget = pose;
        this.altCameraToTarget = altPose;
        this.targetCorners = corners;
        this.poseAmbiguity = ambiguity;
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

    /** Get the Fiducial ID, or -1 if not set. */
    public int getFiducialId() {
        return fiducialId;
    }

    /**
     * Get the ratio of pose reprojection errors, called ambiguity. Numbers above 0.2 are likely to be
     * ambiguous. -1 if invalid.
     */
    public double getPoseAmbiguity() {
        return poseAmbiguity;
    }

    /**
     * Return a list of the 4 corners in image space (origin top left, x left, y down), in no
     * particular order, of the minimum area bounding rectangle of this target
     */
    public List<TargetCorner> getCorners() {
        return targetCorners;
    }

    /**
     * Get the transform that maps camera space (X = forward, Y = left, Z = up) to object/fiducial tag
     * space (X forward, Y left, Z up) with the lowest reprojection error
     */
    public Transform3d getBestCameraToTarget() {
        return bestCameraToTarget;
    }

    /**
     * Get the transform that maps camera space (X = forward, Y = left, Z = up) to object/fiducial tag
     * space (X forward, Y left, Z up) with the highest reprojection error
     */
    public Transform3d getAlternateCameraToTarget() {
        return altCameraToTarget;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhotonTrackedTarget that = (PhotonTrackedTarget) o;
        return Double.compare(that.yaw, yaw) == 0
                && Double.compare(that.pitch, pitch) == 0
                && Double.compare(that.area, area) == 0
                && Objects.equals(bestCameraToTarget, that.bestCameraToTarget)
                && Objects.equals(altCameraToTarget, that.altCameraToTarget)
                && Objects.equals(targetCorners, that.targetCorners);
    }

    @Override
    public int hashCode() {
        return Objects.hash(yaw, pitch, area, bestCameraToTarget, altCameraToTarget);
    }

    private static Transform3d decodeTransform(Packet packet) {
        double x = packet.decodeDouble();
        double y = packet.decodeDouble();
        double z = packet.decodeDouble();
        var translation = new Translation3d(x, y, z);
        double w = packet.decodeDouble();
        x = packet.decodeDouble();
        y = packet.decodeDouble();
        z = packet.decodeDouble();
        var rotation = new Rotation3d(new Quaternion(w, x, y, z));
        return new Transform3d(translation, rotation);
    }

    private static void encodeTransform(Packet packet, Transform3d transform) {
        packet.encode(transform.getTranslation().getX());
        packet.encode(transform.getTranslation().getY());
        packet.encode(transform.getTranslation().getZ());
        packet.encode(transform.getRotation().getQuaternion().getW());
        packet.encode(transform.getRotation().getQuaternion().getX());
        packet.encode(transform.getRotation().getQuaternion().getY());
        packet.encode(transform.getRotation().getQuaternion().getZ());
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
        this.fiducialId = packet.decodeInt();

        this.bestCameraToTarget = decodeTransform(packet);
        this.altCameraToTarget = decodeTransform(packet);

        this.poseAmbiguity = packet.decodeDouble();

        this.targetCorners = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            double cx = packet.decodeDouble();
            double cy = packet.decodeDouble();
            targetCorners.add(new TargetCorner(cx, cy));
        }

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
        packet.encode(fiducialId);
        encodeTransform(packet, bestCameraToTarget);
        encodeTransform(packet, altCameraToTarget);
        packet.encode(poseAmbiguity);

        for (int i = 0; i < 4; i++) {
            packet.encode(targetCorners.get(i).x);
            packet.encode(targetCorners.get(i).y);
        }

        return packet;
    }

    @Override
    public String toString() {
        return "PhotonTrackedTarget{"
                + "yaw="
                + yaw
                + ", pitch="
                + pitch
                + ", area="
                + area
                + ", skew="
                + skew
                + ", fiducialId="
                + fiducialId
                + ", cameraToTarget="
                + bestCameraToTarget
                + ", targetCorners="
                + targetCorners
                + '}';
    }
}
