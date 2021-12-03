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
package org.photonvision;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;

public class SimVisionTarget {
    Pose2d targetPos;
    double targetWidthMeters;
    double targetHeightMeters;
    double targetHeightAboveGroundMeters;
    double tgtAreaMeters2;

    /**
    * Different pre-defined vision targets, used in constructor to create a target in its official
    * position from center of feild.
    */
    public enum VisionTarget {
        InfinateRechargeRedAlliancePowerPort,
        InfinateRechargeRedAllianceLoadingBay,
        InfinateRechargeBlueAlliancePowerPort,
        InfinateRechargeBlueAllianceLoadingBay
    }

    /**
    * Describes a vision target located somewhere on the field that your SimVisionSystem can detect.
    *
    * @param targetPos Pose2d of the target on the field. Define it such that, if you are standing on
    *     the middle of the field facing the target, the Y axis points to your left, and the X axis
    *     points away from you.
    * @param targetHeightAboveGroundMeters Height of the target above the field plane, in meters.
    * @param targetWidthMeters Width of the outer bounding box of the target in meters.
    * @param targetHeightMeters Pair Height of the outer bounding box of the target in meters.
    */
    public SimVisionTarget(
            Pose2d targetPos,
            double targetHeightAboveGroundMeters,
            double targetWidthMeters,
            double targetHeightMeters) {
        this.targetPos = targetPos;
        this.targetHeightAboveGroundMeters = targetHeightAboveGroundMeters;
        this.targetWidthMeters = targetWidthMeters;
        this.targetHeightMeters = targetHeightMeters;
        this.tgtAreaMeters2 = targetWidthMeters * targetHeightMeters;
    }

    /**
    * Create a pre-defined target
    *
    * <p>Center of the target is placed as if you were standing at the center of the field facing the
    * blue alliance wall. positive x in front of you, positive y to the left.
    *
    * @param visionTarget the type of target to create
    */
    public SimVisionTarget(VisionTarget visionTarget) {
        switch (visionTarget) {
            case InfinateRechargeRedAlliancePowerPort:
                this.targetPos = new Pose2d(7.9916, -1.7052, new Rotation2d()); // x: 314.63" y: 67.134"
                this.targetHeightAboveGroundMeters = 2.06375; // 6'9.25"
                this.targetWidthMeters = 0.99695; // 3'3.25"
                this.targetHeightMeters = 0.4318; // 1'5"
                this.tgtAreaMeters2 = this.targetWidthMeters * this.targetHeightMeters;
                break;
            case InfinateRechargeRedAllianceLoadingBay:
                this.targetPos = new Pose2d(-7.9916, -1.5494, new Rotation2d()); // x: 314.63" y: 61"
                this.targetHeightAboveGroundMeters = 0.2794; // 11"
                this.targetWidthMeters = 0.1778; // 7"
                this.targetHeightMeters = 0.2794; // 11"
                this.tgtAreaMeters2 = this.targetWidthMeters * this.targetHeightMeters;
                break;
            case InfinateRechargeBlueAlliancePowerPort:
                this.targetPos = new Pose2d(-7.9916, 1.7052, new Rotation2d()); // x: 314.63" y: 67.134"
                this.targetHeightAboveGroundMeters = 2.06375; // 6'9.25"
                this.targetWidthMeters = 0.99695; // 3'3.25"
                this.targetHeightMeters = 0.4318; // 1'5"
                this.tgtAreaMeters2 = this.targetWidthMeters * this.targetHeightMeters;
                break;
            case InfinateRechargeBlueAllianceLoadingBay:
                this.targetPos = new Pose2d(7.9916, 1.5494, new Rotation2d()); // x: 314.63" y: 61"
                this.targetHeightAboveGroundMeters = 0.2794; // 11"
                this.targetWidthMeters = 0.1778; // 7"
                this.targetHeightMeters = 0.2794; // 11"
                this.tgtAreaMeters2 = this.targetWidthMeters * this.targetHeightMeters;
                break;
            default:
                throw new IllegalArgumentException("Invalid VisionTarget");
        }
    }
}
