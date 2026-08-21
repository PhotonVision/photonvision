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

import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.photonvision.common.util.numbers.DoubleCouple;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.opencv.ImageRotationMode;
import org.wpilib.vision.camera.VideoMode;

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

    // The crop rectangle rarely changes between frames, so cache the last cropped result to avoid
    // reallocating native calibration memory every frame.
    private Rect cachedCropRect = null;
    private FrameStaticProperties cachedCropStaticProperties = null;

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

    /**
     * Instantiates frame static properties with explicit optical parameters, bypassing the pinhole
     * derivation. Used when transforming existing properties (e.g. cropping) where the focal lengths
     * and principal point are already known and must be preserved rather than recomputed.
     */
    private FrameStaticProperties(
            int imageWidth,
            int imageHeight,
            double fov,
            double horizontalFocalLength,
            double verticalFocalLength,
            double centerX,
            double centerY,
            CameraCalibrationCoefficients cal) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.fov = fov;
        this.imageArea = imageWidth * imageHeight;
        this.horizontalFocalLength = horizontalFocalLength;
        this.verticalFocalLength = verticalFocalLength;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerPoint = new Point(centerX, centerY);
        this.cameraCalibration = cal;
    }

    /**
     * Produce frame static properties for a statically-cropped image. Cropping shrinks the image and
     * shifts the origin to the crop's top-left corner, so the principal point shifts by the crop
     * origin while the focal lengths (which depend on the lens, not the framing) are preserved.
     *
     * @param cropRect The crop rectangle, in pixel coordinates of this image. Must lie within the
     *     image bounds. A null rectangle is treated as a no-op.
     * @return Static properties describing the cropped image.
     */
    public FrameStaticProperties crop(Rect cropRect) {
        if (cropRect == null) {
            // Cropping is disabled, so the cached crop can never be reused; don't hold its native
            // calibration memory alive until the next crop happens to come along.
            releaseCachedCrop();
            return this;
        }

        if (cropRect.equals(cachedCropRect)) {
            return cachedCropStaticProperties;
        }

        FrameStaticProperties cropped;
        if (cameraCalibration != null) {
            // Derive optical parameters from the shifted intrinsics so everything stays self
            // consistent with the cropped calibration used for pose estimation.
            cropped =
                    new FrameStaticProperties(
                            cropRect.width, cropRect.height, fov, cameraCalibration.cropCoefficients(cropRect));
        } else {
            // No calibration: keep the focal lengths (the lens is unchanged) and shift the principal
            // point to match the new image origin.
            cropped =
                    new FrameStaticProperties(
                            cropRect.width,
                            cropRect.height,
                            fov,
                            horizontalFocalLength,
                            verticalFocalLength,
                            centerX - cropRect.x,
                            centerY - cropRect.y,
                            null);
        }

        // The crop rect changed, so the previously cached properties are now garbage -- free the
        // native calibration memory they own. The frame provider that calls this and the pipeline that
        // consumes the frame run on the same thread, so the frame built from the superseded properties
        // has been fully processed by the time a new crop rect arrives here.
        releaseCachedCrop();

        cachedCropRect = cropRect.clone();
        cachedCropStaticProperties = cropped;
        return cropped;
    }

    /**
     * Discard the cached cropped properties, releasing the derived calibration coefficients they own.
     */
    private void releaseCachedCrop() {
        if (cachedCropStaticProperties != null
                && cachedCropStaticProperties.cameraCalibration != null
                // Only release coefficients derived here -- never the ones this instance borrowed from
                // its camera, which outlive any single crop.
                && cachedCropStaticProperties.cameraCalibration != cameraCalibration) {
            cachedCropStaticProperties.cameraCalibration.release();
        }

        cachedCropRect = null;
        cachedCropStaticProperties = null;
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
