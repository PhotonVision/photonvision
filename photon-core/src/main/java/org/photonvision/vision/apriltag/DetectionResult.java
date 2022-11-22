/*
Copyright (c) 2022 Photon Vision. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
   * Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
   * Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
   * Neither the name of FIRST, WPILib, nor the names of other WPILib
     contributors may be used to endorse or promote products derived from
     this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY FIRST AND OTHER WPILIB CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY NONINFRINGEMENT AND FITNESS FOR A PARTICULAR
PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL FIRST OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.photonvision.vision.apriltag;

import edu.wpi.first.math.MatBuilder;
import edu.wpi.first.math.Nat;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import java.util.Arrays;

import org.photonvision.common.util.math.MathUtils;

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
                        new Rotation3d(MathUtils.orthogonalizeRotationMatrix(new MatBuilder<>(Nat.N3(), Nat.N3()).fill(pose1RotArr))));
        this.error2 = err2;
        this.poseResult2 =
                new Transform3d(
                        new Translation3d(pose2TransArr[0], pose2TransArr[1], pose2TransArr[2]),
                        new Rotation3d(MathUtils.orthogonalizeRotationMatrix(new MatBuilder<>(Nat.N3(), Nat.N3()).fill(pose2RotArr))));
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
