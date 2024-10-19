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

package org.photonvision.vision.opencv;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.util.Units;
import org.opencv.core.Core;
import org.opencv.core.Point;

/**
 * An image rotation about the camera's +Z axis, which points out of the camera towards the world.
 * This is mirrored relative to what you might traditionally think of as image rotation, which is
 * about an axis coming out of the image towards the viewer or camera. TODO: pull this from
 * image-rotation.md
 */
public enum ImageRotationMode {
    DEG_0(-1, new Rotation2d()),
    // rotating an image matrix clockwise is a ccw rotation about camera +Z, lmao
    DEG_90_CCW(Core.ROTATE_90_COUNTERCLOCKWISE, new Rotation2d(Units.degreesToRadians(90))),
    DEG_180_CCW(Core.ROTATE_180, new Rotation2d(Units.degreesToRadians(180))),
    DEG_270_CCW(Core.ROTATE_90_CLOCKWISE, new Rotation2d(Units.degreesToRadians(-90)));

    public final int value;
    public final Rotation2d rotation2d;

    private ImageRotationMode(int value, Rotation2d tr) {
        this.value = value;
        this.rotation2d = tr;
    }

    /**
     * Rotate a point in an image
     *
     * @param point The point in the unrotated image
     * @param width Image width, in pixels
     * @param height Image height, in pixels
     * @return The point in the rotated frame
     */
    public Point rotatePoint(Point point, double width, double height) {
        Pose2d offset;
        switch (this) {
            case DEG_0:
                return point;
            case DEG_90_CCW:
                offset = new Pose2d(width, 0, rotation2d);
                break;
            case DEG_180_CCW:
                offset = new Pose2d(width, height, rotation2d);
                break;
            case DEG_270_CCW:
                offset = new Pose2d(0, height, rotation2d);
                break;
            default:
                throw new RuntimeException("Totally bjork");
        }

        var pointAsPose = new Pose2d(point.x, point.y, new Rotation2d());
        var ret = pointAsPose.relativeTo(offset);

        return new Point(ret.getX(), ret.getY());
    }
}
