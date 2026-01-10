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

/** Information about a detected target. */
public class PhotonTrackedTarget
        implements ProtobufSerializable, PhotonStructSerializable<PhotonTrackedTarget> {
    private static final int MAX_CORNERS = 8;

    /** The yaw of the target in degrees, with left being the positive direction. */
    public double yaw;

    /** The pitch of the target in degrees, with up being the positive direction. */
    public double pitch;

    /** The area (how much of the camera feed the bounding box takes up) as a percentage (0-100). */
    public double area;

    /** The skew of the target in degrees, with counterclockwise being the positive direction. */
    public double skew;

    /** The fiducial ID, or -1 if it doesn't exist for this target. */
    public int fiducialId;

    /** The object detection class ID, or -1 if it doesn't exist for this target. */
    public int objDetectId;

    /** The object detection confidence, or -1 if it doesn't exist for this target. */
    public float objDetectConf;

    /** The transform with the lowest reprojection error */
    public Transform3d bestCameraToTarget;

    /** The transform with the highest reprojection error */
    public Transform3d altCameraToTarget;

    /** The ratio (best:alt) of reprojection errors */
    public double poseAmbiguity;

    /** Corners from the min-area rectangle bounding the target */
    public List<TargetCorner> minAreaRectCorners;

    /** Corners from the corner detection method used */
    public List<TargetCorner> detectedCorners;

    /**
     * Construct a tracked target, given exactly 4 corners
     *
     * @param yaw The yaw of the target
     * @param pitch The pitch of the target
     * @param area The area of the target as a percentage of the camera image
     * @param skew The skew of the target
     * @param fiducialId The fiduical tag ID
     * @param classId The object detection class ID
     * @param objDetectConf The object detection confidence
     * @param pose The best camera to target transform
     * @param altPose The alternate camera to target transform
     * @param ambiguity The ambiguity (best:alternate ratio of reprojection errors) of the target
     * @param minAreaRectCorners The corners of minimum area bounding box of the target
     * @param detectedCorners The detected corners of the target
     */
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

    /** Used for serialization. */
    public PhotonTrackedTarget() {}

    public double getYaw() {
        return yaw;
    }

    public double getPitch() {
        return pitch;
    }

    /**
     * The area (how much of the camera feed the bounding box takes up) as a percentage (0-100).
     *
     * @return The area as a percentage
     */
    public double getArea() {
        return area;
    }

    public double getSkew() {
        return skew;
    }

    /**
     * Get the fiducial ID, or -1 if it doesn't exist for this target.
     *
     * @return The fiducial ID
     */
    public int getFiducialId() {
        return fiducialId;
    }

    /**
     * Get the object detection class ID number, or -1 if it doesn't exist for this target.
     *
     * @return The object detection class ID
     */
    public int getDetectedObjectClassID() {
        return objDetectId;
    }

    /**
     * Get the object detection confidence, or -1 if it doesn't exist for this target. This will be
     * between 0 and 1, with 1 indicating most confidence, and 0 least.
     *
     * @return The object detection confidence
     */
    public float getDetectedObjectConfidence() {
        return objDetectConf;
    }

    /**
     * Get the ratio of best:alternate pose reprojection errors, called ambiguity. This is between 0
     * and 1 (0 being no ambiguity, and 1 meaning both have the same reprojection error). Numbers
     * above 0.2 are likely to be ambiguous. -1 if invalid.
     *
     * @return The pose ambiguity
     */
    public double getPoseAmbiguity() {
        return poseAmbiguity;
    }

    /**
     * Return a list of the 4 corners in image space (origin top left, x right, y down), in no
     * particular order, of the minimum area bounding rectangle of this target
     *
     * @return The list of corners
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
     *
     * @return The list of detected corners for this target
     */
    public List<TargetCorner> getDetectedCorners() {
        return detectedCorners;
    }

    /**
     * Get the transform that maps camera space (X = forward, Y = left, Z = up) to object/fiducial tag
     * space (X forward, Y left, Z up) with the lowest reprojection error
     *
     * @return The transform with the lowest reprojection error (the best)
     */
    public Transform3d getBestCameraToTarget() {
        return bestCameraToTarget;
    }

    /**
     * Get the transform that maps camera space (X = forward, Y = left, Z = up) to object/fiducial tag
     * space (X forward, Y left, Z up) with the highest reprojection error
     *
     * @return The transform with the highest reprojection error (the alternate)
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
        result = prime * result + objDetectId;
        result = prime * result + Float.floatToIntBits(objDetectConf);
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
        if (objDetectId != other.objDetectId) return false;
        if (Float.floatToIntBits(objDetectConf) != Float.floatToIntBits(other.objDetectConf))
            return false;
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
        return "PhotonTrackedTarget [yaw="
                + yaw
                + ", pitch="
                + pitch
                + ", area="
                + area
                + ", skew="
                + skew
                + ", fiducialId="
                + fiducialId
                + ", objDetectId="
                + objDetectId
                + ", objDetectConf="
                + objDetectConf
                + ", bestCameraToTarget="
                + bestCameraToTarget
                + ", altCameraToTarget="
                + altCameraToTarget
                + ", poseAmbiguity="
                + poseAmbiguity
                + ", minAreaRectCorners="
                + minAreaRectCorners
                + ", detectedCorners="
                + detectedCorners
                + "]";
    }

    /** PhotonTrackedTarget protobuf for serialization. */
    public static final PhotonTrackedTargetProto proto = new PhotonTrackedTargetProto();

    /** PhotonTrackedTarget PhotonStruct for serialization. */
    public static final PhotonTrackedTargetSerde photonStruct = new PhotonTrackedTargetSerde();

    @Override
    public PacketSerde<PhotonTrackedTarget> getSerde() {
        return photonStruct;
    }
}
