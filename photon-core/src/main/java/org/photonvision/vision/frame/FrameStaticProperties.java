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
import org.photonvision.vision.opencv.ImageRotationMode;

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

    // CameraCalibrationCoefficients hold native memory, so cache them here to avoid extra allocations
    private final FrameStaticProperties[] cachedRotationStaticProperties =
            new FrameStaticProperties[4];

    /**
     * Instantiates a new Frame static properties.
     *
     * @param mode The Video Mode of the camera.
     * @param fov The FOV (Field Of Vision) of the image in degrees.
     */
    public FrameStaticProperties(VideoMode mode, double fov, CameraCalibrationCoefficients cal) {
        this(mode != null ? mode.width : 1, mode != null ? mode.height : 1, fov, cal);
    }

    /**
     * Instantiates a new Frame static properties.
     *
     * @param imageWidth The width of the image in pixels.
     * @param imageHeight The width of the image in pixels.
     * @param fov The FOV (Field Of Vision) of the image in degrees.
     */
    public FrameStaticProperties(
            int imageWidth, int imageHeight, double fov, CameraCalibrationCoefficients cal) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.fov = fov;
        this.cameraCalibration = cal;

        imageArea = this.imageWidth * this.imageHeight;

        // pinhole model calculations
        if (cameraCalibration != null && cameraCalibration.getCameraIntrinsicsMat() != null) {
            // Use calibration data
            var camIntrinsics = cameraCalibration.getCameraIntrinsicsMat();
            centerX = camIntrinsics.get(0, 2)[0];
            centerY = camIntrinsics.get(1, 2)[0];
            centerPoint = new Point(centerX, centerY);
            horizontalFocalLength = camIntrinsics.get(0, 0)[0];
            verticalFocalLength = camIntrinsics.get(1, 1)[0];
        } else {
            // No calibration data. Calculate from user provided diagonal FOV
            centerX = (this.imageWidth / 2.0) - 0.5;
            centerY = (this.imageHeight / 2.0) - 0.5;
            centerPoint = new Point(centerX, centerY);

            DoubleCouple horizVertViews =
                    calculateHorizontalVerticalFoV(this.fov, this.imageWidth, this.imageHeight);
            double horizFOV = Math.toRadians(horizVertViews.getFirst());
            double vertFOV = Math.toRadians(horizVertViews.getSecond());
            horizontalFocalLength = (this.imageWidth / 2.0) / Math.tan(horizFOV / 2.0);
            verticalFocalLength = (this.imageHeight / 2.0) / Math.tan(vertFOV / 2.0);
        }
    }

    public FrameStaticProperties rotate(ImageRotationMode rotation) {
        if (rotation == ImageRotationMode.DEG_0) {
            return this;
        }

        int newWidth = imageWidth;
        int newHeight = imageHeight;

        if (rotation == ImageRotationMode.DEG_90_CCW || rotation == ImageRotationMode.DEG_270_CCW) {
            newWidth = imageHeight;
            newHeight = imageWidth;
        }

        if (cameraCalibration == null) {
            return new FrameStaticProperties(newWidth, newHeight, fov, null);
        }

        if (cachedRotationStaticProperties[rotation.ordinal()] == null) {
            cachedRotationStaticProperties[rotation.ordinal()] =
                    new FrameStaticProperties(
                            newWidth, newHeight, fov, cameraCalibration.rotateCoefficients(rotation));
        }

        return cachedRotationStaticProperties[rotation.ordinal()];
    }

    /**
     * Calculates the horizontal and vertical FOV components from a given diagonal FOV and image size.
     *
     * @param diagonalFoV Diagonal FOV in degrees
     * @param imageWidth Image width in pixels
     * @param imageHeight Image height in pixels
     * @return Horizontal and vertical FOV in degrees
     */
    public static DoubleCouple calculateHorizontalVerticalFoV(
            double diagonalFoV, int imageWidth, int imageHeight) {
        diagonalFoV = Math.toRadians(diagonalFoV);
        double diagonalAspect = Math.hypot(imageWidth, imageHeight);

        double horizontalView =
                Math.atan(Math.tan(diagonalFoV / 2) * (imageWidth / diagonalAspect)) * 2;
        double verticalView = Math.atan(Math.tan(diagonalFoV / 2) * (imageHeight / diagonalAspect)) * 2;

        return new DoubleCouple(Math.toDegrees(horizontalView), Math.toDegrees(verticalView));
    }
}
