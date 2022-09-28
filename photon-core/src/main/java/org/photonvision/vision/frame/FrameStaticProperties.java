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

package org.photonvision.vision.frame;

import edu.wpi.first.cscore.VideoMode;
import org.opencv.core.Point;
import org.photonvision.common.util.numbers.DoubleCouple;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;

/** Represents the properties of a frame. */
public class FrameStaticProperties {
    public final int imageWidth;
    public final int imageHeight;
    public final double fov;
    public final double imageArea;
    public final double centerX;
    public final double centerY;
    public final Point centerPoint;
    public final double horizontalFocalLength;
    public final double verticalFocalLength;
    public CameraCalibrationCoefficients cameraCalibration;

    /**
     * Instantiates a new Frame static properties.
     *
     * @param mode The Video Mode of the camera.
     * @param fov The fov of the image.
     */
    public FrameStaticProperties(VideoMode mode, double fov, CameraCalibrationCoefficients cal) {
        this(mode != null ? mode.width : 1, mode != null ? mode.height : 1, fov, cal);
    }

    /**
     * Instantiates a new Frame static properties.
     *
     * @param imageWidth The width of the image.
     * @param imageHeight The width of the image.
     * @param fov The fov of the image.
     */
    public FrameStaticProperties(
            int imageWidth, int imageHeight, double fov, CameraCalibrationCoefficients cal) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.fov = fov;
        this.cameraCalibration = cal;

        imageArea = this.imageWidth * this.imageHeight;

        centerX = ((double) this.imageWidth / 2) - 0.5;
        centerY = ((double) this.imageHeight / 2) - 0.5;
        centerPoint = new Point(centerX, centerY);

        // pinhole model calculations
        DoubleCouple horizVertViews =
                calculateHorizontalVerticalFoV(this.fov, this.imageWidth, this.imageHeight);

        horizontalFocalLength = this.imageWidth / (2 * Math.tan(horizVertViews.getFirst() / 2));
        verticalFocalLength = this.imageHeight / (2 * Math.tan(horizVertViews.getSecond() / 2));
    }

    public static DoubleCouple calculateHorizontalVerticalFoV(
            double diagonalFoV, int imageWidth, int imageHeight) {
        double diagonalView = Math.toRadians(diagonalFoV);
        double diagonalAspect = Math.hypot(imageWidth, imageHeight);

        double horizontalView =
                Math.atan(Math.tan(diagonalView / 2) * (imageWidth / diagonalAspect)) * 2;
        double verticalView =
                Math.atan(Math.tan(diagonalView / 2) * (imageHeight / diagonalAspect)) * 2;

        return new DoubleCouple(horizontalView, verticalView);
    }
}
