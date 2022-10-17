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

package org.photonvision.vision.apriltag;

import edu.wpi.first.math.MatBuilder;
import edu.wpi.first.math.Nat;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import java.util.Arrays;

public class DetectionResult {
    public int getId() {
        return id;
    }

    public int getHamming() {
        return hamming;
    }

    public float getDecisionMargin() {
        return decision_margin;
    }

    public void setDecisionMargin(float decision_margin) {
        this.decision_margin = decision_margin;
    }

    public double[] getHomography() {
        return homography;
    }

    public void setHomography(double[] homography) {
        this.homography = homography;
    }

    public double getCenterX() {
        return centerX;
    }

    public void setCenterX(double centerX) {
        this.centerX = centerX;
    }

    public double getCenterY() {
        return centerY;
    }

    public void setCenterY(double centerY) {
        this.centerY = centerY;
    }

    public double[] getCorners() {
        return corners;
    }

    public void setCorners(double[] corners) {
        this.corners = corners;
    }

    public double getError1() {
        return error1;
    }

    public double getError2() {
        return error2;
    }

    public Transform3d getPoseResult1() {
        return poseResult1;
    }

    public Transform3d getPoseResult2() {
        return poseResult2;
    }

    int id;
    int hamming;
    float decision_margin;
    double[] homography;
    double centerX, centerY;
    double[] corners;

    Transform3d poseResult1;
    double error1;
    Transform3d poseResult2;
    double error2;

    public DetectionResult(
            int id,
            int hamming,
            float decision_margin,
            double[] homography,
            double centerX,
            double centerY,
            double[] corners,
            double[] pose1TransArr,
            double[] pose1RotArr,
            double err1,
            double[] pose2TransArr,
            double[] pose2RotArr,
            double err2) {
        this.id = id;
        this.hamming = hamming;
        this.decision_margin = decision_margin;
        this.homography = homography;
        this.centerX = centerX;
        this.centerY = centerY;
        this.corners = corners;

        this.error1 = err1;
        this.poseResult1 =
                new Transform3d(
                        new Translation3d(pose1TransArr[0], pose1TransArr[1], pose1TransArr[2]),
                        new Rotation3d(new MatBuilder<>(Nat.N3(), Nat.N3()).fill(pose1RotArr)));
        this.error2 = err2;
        this.poseResult2 =
                new Transform3d(
                        new Translation3d(pose2TransArr[0], pose2TransArr[1], pose2TransArr[2]),
                        new Rotation3d(new MatBuilder<>(Nat.N3(), Nat.N3()).fill(pose2RotArr)));
    }

    /**
     * Get the ratio of pose reprojection errors, called ambiguity. Numbers above 0.2 are likely to be
     * ambiguous.
     */
    public double getPoseAmbiguity() {
        var min = Math.min(error1, error2);
        var max = Math.max(error1, error2);

        if (max > 0) {
            return min / max;
        } else {
            return -1;
        }
    }

    @Override
    public String toString() {
        return "DetectionResult [centerX="
                + centerX
                + ", centerY="
                + centerY
                + ", corners="
                + Arrays.toString(corners)
                + ", decision_margin="
                + decision_margin
                + ", error1="
                + error1
                + ", error2="
                + error2
                + ", hamming="
                + hamming
                + ", homography="
                + Arrays.toString(homography)
                + ", id="
                + id
                + ", poseResult1="
                + poseResult1
                + ", poseResult2="
                + poseResult2
                + "]";
    }
}
