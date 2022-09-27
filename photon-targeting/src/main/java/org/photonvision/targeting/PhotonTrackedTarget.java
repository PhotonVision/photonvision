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
    public static final int PACK_SIZE_BYTES = Double.BYTES * (5 + 7 + 2 * 4);

    private double yaw;
    private double pitch;
    private double area;
    private double skew;
    private int fiducialId;
    private Transform3d cameraToTarget = new Transform3d();
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
            List<TargetCorner> corners) {
        assert corners.size() == 4;
        this.yaw = yaw;
        this.pitch = pitch;
        this.area = area;
        this.skew = skew;
        this.fiducialId = id;
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

    public int getFiducialId() {
        return fiducialId;
    }

    /**
     * Return a list of the 4 corners in image space (origin top left, x left, y down), in no
     * particular order, of the minimum area bounding rectangle of this target
     */
    public List<TargetCorner> getCorners() {
        return targetCorners;
    }

    public Transform3d getCameraToTarget() {
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
        this.fiducialId = packet.decodeInt();

        double x = packet.decodeDouble();
        double y = packet.decodeDouble();
        double z = packet.decodeDouble();
        var translation = new Translation3d(x, y, z);
        double w = packet.decodeDouble();
        x = packet.decodeDouble();
        y = packet.decodeDouble();
        z = packet.decodeDouble();
        var rotation = new Rotation3d(new Quaternion(w, x, y, z));

        this.targetCorners = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            double cx = packet.decodeDouble();
            double cy = packet.decodeDouble();
            targetCorners.add(new TargetCorner(cx, cy));
        }

        this.cameraToTarget = new Transform3d(translation, rotation);

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
        packet.encode(cameraToTarget.getTranslation().getX());
        packet.encode(cameraToTarget.getTranslation().getY());
        packet.encode(cameraToTarget.getTranslation().getZ());
        packet.encode(cameraToTarget.getRotation().getQuaternion().getW());
        packet.encode(cameraToTarget.getRotation().getQuaternion().getX());
        packet.encode(cameraToTarget.getRotation().getQuaternion().getY());
        packet.encode(cameraToTarget.getRotation().getQuaternion().getZ());

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
                + cameraToTarget
                + ", targetCorners="
                + targetCorners
                + '}';
    }
}
