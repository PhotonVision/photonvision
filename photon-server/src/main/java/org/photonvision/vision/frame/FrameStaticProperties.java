/*
 * Copyright (C) 2020 Photon Vision.
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

import edu.wpi.cscore.VideoMode;
import org.apache.commons.math3.fraction.Fraction;
import org.apache.commons.math3.util.FastMath;
import org.opencv.core.Point;

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

    /**
    * Instantiates a new Frame static properties.
    *
    * @param mode The Video Mode of the camera.
    * @param fov The fov of the image.
    */
    public FrameStaticProperties(VideoMode mode, double fov) {
        this(mode.width, mode.height, fov);
    }

    /**
    * Instantiates a new Frame static properties.
    *
    * @param imageWidth The width of the image.
    * @param imageHeight The width of the image.
    * @param fov The fov of the image.
    */
    public FrameStaticProperties(int imageWidth, int imageHeight, double fov) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.fov = fov;

        imageArea = this.imageWidth * this.imageHeight;

        centerX = ((double) this.imageWidth / 2) - 0.5;
        centerY = ((double) this.imageHeight / 2) - 0.5;
        centerPoint = new Point(centerX, centerY);

        // pinhole model calculations
        double diagonalView = FastMath.toRadians(this.fov);
        Fraction aspectFraction = new Fraction(this.imageWidth, this.imageHeight);

        int horizontalRatio = aspectFraction.getNumerator();
        int verticalRatio = aspectFraction.getDenominator();

        double diagonalAspect = FastMath.hypot(horizontalRatio, verticalRatio);
        double horizontalView =
                FastMath.atan(FastMath.tan(diagonalView / 2) * (horizontalRatio / diagonalAspect)) * 2;
        double verticalView =
                FastMath.atan(FastMath.tan(diagonalView / 2) * (verticalRatio / diagonalAspect)) * 2;

        horizontalFocalLength = this.imageWidth / (2 * FastMath.tan(horizontalView / 2));
        verticalFocalLength = this.imageHeight / (2 * FastMath.tan(verticalView / 2));
    }
}
