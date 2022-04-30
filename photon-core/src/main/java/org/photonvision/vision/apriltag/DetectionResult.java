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

public class DetectionResult{
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

    int id;
    int hamming;
    float decision_margin;
    double[] homography;
    double centerX, centerY;
    double[] corners;

    public DetectionResult(int id, int hamming, float decision_margin, double[] homography, double centerX,
            double centerY, double[] corners) {
        this.id = id;
        this.hamming = hamming;
        this.decision_margin = decision_margin;
        this.homography = homography;
        this.centerX = centerX;
        this.centerY = centerY;
        this.corners = corners;
    }

    @Override
    public String toString() {
        return "ID " + id + " ham " + hamming + " decision margin " + decision_margin
                + " homography " + homography + " cx " + centerX + " cy " + centerY
                + " corners " + corners;
    }
}
