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

import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.util.protobuf.ProtobufSerializable;
import java.util.List;
import org.photonvision.common.dataflow.structures.PacketSerde;
import org.photonvision.struct.PhotonTrackedTargetSerde;
import org.photonvision.targeting.proto.PhotonTrackedTargetProto;
import org.photonvision.targeting.serde.PhotonStructSerializable;

public class PhotonTrackedTarget
        implements ProtobufSerializable, PhotonStructSerializable<PhotonTrackedTarget> {
    private static final int MAX_CORNERS = 8;

    public double yaw;
    public double pitch;
    public double area;
    public double skew;
    public int fiducialId;
    public int objDetectId;
    public float objDetectConf;
    public Transform3d bestCameraToTarget;
    public Transform3d altCameraToTarget;
    public double poseAmbiguity;

    // Corners from the min-area rectangle bounding the target
    public List<TargetCorner> minAreaRectCorners;

    // Corners from whatever corner detection method was used
    public List<TargetCorner> detectedCorners;

    /** Construct a tracked target, given exactly 4 corners */
    public PhotonTrackedTarget(
            double yaw,
            double pitch,
            double area,
            double skew,
            int fiducialId,
            int classId,
            float objDetectConf,
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
        this.fiducialId = fiducialId;
        this.objDetectId = classId;
        this.objDetectConf = objDetectConf;
        this.bestCameraToTarget = pose;
        this.altCameraToTarget = altPose;
        this.minAreaRectCorners = minAreaRectCorners;
        this.detectedCorners = detectedCorners;
        this.poseAmbiguity = ambiguity;
    }

    public PhotonTrackedTarget() {
        // TODO Auto-generated constructor stub
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

    /** Get the fiducial ID, or -1 if not set. */
    public int getFiducialId() {
        return fiducialId;
    }

    /** Get the object detection class ID number, or -1 if not set. */
    public int getDetectedObjectClassID() {
        return objDetectId;
    }

    /**
     * Get the object detection confidence, or -1 if not set. This will be between 0 and 1, with 1
     * indicating most confidence, and 0 least.
     */
    public float getDetectedObjectConfidence() {
        return objDetectConf;
    }

    /**
     * Get the ratio of best:alternate pose reprojection errors, called ambiguity. This is betweeen 0
     * and 1 (0 being no ambiguity, and 1 meaning both have the same reprojection error). Numbers
     * above 0.2 are likely to be ambiguous. -1 if invalid.
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
     * <p>For fiducials, the order is known and is always counter-clock wise around the tag, like so:
     *
     * <pre>​
     * ⟶ +X  3 ----- 2
     * |      |       |
     * V      |       |
     * +Y     0 ----- 1
     * </pre>
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

    public static final PhotonTrackedTargetProto proto = new PhotonTrackedTargetProto();
    public static final PhotonTrackedTargetSerde photonStruct = new PhotonTrackedTargetSerde();

    @Override
    public PacketSerde<PhotonTrackedTarget> getSerde() {
        return photonStruct;
    }
}
