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
import org.photonvision.common.dataflow.structures.Packet;

public class PhotonTrackedTarget {
    private static final int MAX_CORNERS = 8;
    public static final int PACK_SIZE_BYTES =
            Double.BYTES * (5 + 7 + 2 * 4 + 1 + 7 + 2 * MAX_CORNERS);

    private double yaw;
    private double pitch;
    private double area;
    private double skew;
    private int fiducialId;
    private Transform3d bestCameraToTarget = new Transform3d();
    private Transform3d altCameraToTarget = new Transform3d();
    private double poseAmbiguity;

    // Corners from the min-area rectangle bounding the target
    private List<TargetCorner> minAreaRectCorners;

    // Corners from whatever corner detection method was used
    private List<TargetCorner> detectedCorners;

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
            List<TargetCorner> minAreaRectCorners,
            List<TargetCorner> detectedCorners) {
        assert minAreaRectCorners.size() == 4;

        if (detectedCorners.size() > MAX_CORNERS) {
            detectedCorners = detectedCorners.subList(0, MAX_CORNERS);
        }

        this.yaw = yaw;
        this.pitch = pitch;
        this.area = area;
        this.skew = skew;
        this.fiducialId = id;
        this.bestCameraToTarget = pose;
        this.altCameraToTarget = altPose;
        this.minAreaRectCorners = minAreaRectCorners;
        this.detectedCorners = detectedCorners;
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
     * Return a list of the 4 corners in image space (origin top left, x right, y down), in no
     * particular order, of the minimum area bounding rectangle of this target
     */
    public List<TargetCorner> getMinAreaRectCorners() {
        return minAreaRectCorners;
    }

    /**
     * Return a list of the n corners in image space (origin top left, x right, y down), in no
     * particular order, detected for this target.
     *
     * <p>For fiducials, the order is known and is always counter-clock wise around the tag, like so
     *
     * <p>spotless:off
     * -> +X  3 ----- 2
     * |      |       |
     * V      |       |
     * +Y     0 ----- 1
     * spotless:on
     */
    public List<TargetCorner> getDetectedCorners() {
        return detectedCorners;
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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(yaw);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(pitch);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(area);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(skew);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + fiducialId;
        result = prime * result + ((bestCameraToTarget == null) ? 0 : bestCameraToTarget.hashCode());
        result = prime * result + ((altCameraToTarget == null) ? 0 : altCameraToTarget.hashCode());
        temp = Double.doubleToLongBits(poseAmbiguity);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((minAreaRectCorners == null) ? 0 : minAreaRectCorners.hashCode());
        result = prime * result + ((detectedCorners == null) ? 0 : detectedCorners.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        PhotonTrackedTarget other = (PhotonTrackedTarget) obj;
        if (Double.doubleToLongBits(yaw) != Double.doubleToLongBits(other.yaw)) return false;
        if (Double.doubleToLongBits(pitch) != Double.doubleToLongBits(other.pitch)) return false;
        if (Double.doubleToLongBits(area) != Double.doubleToLongBits(other.area)) return false;
        if (Double.doubleToLongBits(skew) != Double.doubleToLongBits(other.skew)) return false;
        if (fiducialId != other.fiducialId) return false;
        if (bestCameraToTarget == null) {
            if (other.bestCameraToTarget != null) return false;
        } else if (!bestCameraToTarget.equals(other.bestCameraToTarget)) return false;
        if (altCameraToTarget == null) {
            if (other.altCameraToTarget != null) return false;
        } else if (!altCameraToTarget.equals(other.altCameraToTarget)) return false;
        if (Double.doubleToLongBits(poseAmbiguity) != Double.doubleToLongBits(other.poseAmbiguity))
            return false;
        if (minAreaRectCorners == null) {
            if (other.minAreaRectCorners != null) return false;
        } else if (!minAreaRectCorners.equals(other.minAreaRectCorners)) return false;
        if (detectedCorners == null) {
            if (other.detectedCorners != null) return false;
        } else if (!detectedCorners.equals(other.detectedCorners)) return false;
        return true;
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

    private static void encodeList(Packet packet, List<TargetCorner> list) {
        packet.encode((byte) Math.min(list.size(), Byte.MAX_VALUE));
        for (int i = 0; i < list.size(); i++) {
            packet.encode(list.get(i).x);
            packet.encode(list.get(i).y);
        }
    }

    private static List<TargetCorner> decodeList(Packet p) {
        byte len = p.decodeByte();
        var ret = new ArrayList<TargetCorner>();
        for (int i = 0; i < len; i++) {
            double cx = p.decodeDouble();
            double cy = p.decodeDouble();
            ret.add(new TargetCorner(cx, cy));
        }
        return ret;
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

        this.minAreaRectCorners = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            double cx = packet.decodeDouble();
            double cy = packet.decodeDouble();
            minAreaRectCorners.add(new TargetCorner(cx, cy));
        }

        detectedCorners = decodeList(packet);

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
            packet.encode(minAreaRectCorners.get(i).x);
            packet.encode(minAreaRectCorners.get(i).y);
        }

        encodeList(packet, detectedCorners);

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
                + minAreaRectCorners
                + '}';
    }
}
