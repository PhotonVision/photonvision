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

package org.photonvision.apple;

/**
 * Stub implementation of DetectionResult for non-macOS platforms. This class is generated from
 * Swift code on macOS.
 */
public final class DetectionResult {
    private DetectionResult() {}

    public static DetectionResult init(
            double x,
            double y,
            double width,
            double height,
            int classId,
            double confidence,
            Object swiftArena) {
        throw new UnsupportedOperationException("Apple object detection is only supported on macOS");
    }

    public double getX() {
        throw new UnsupportedOperationException("Apple object detection is only supported on macOS");
    }

    public double getY() {
        throw new UnsupportedOperationException("Apple object detection is only supported on macOS");
    }

    public double getWidth() {
        throw new UnsupportedOperationException("Apple object detection is only supported on macOS");
    }

    public double getHeight() {
        throw new UnsupportedOperationException("Apple object detection is only supported on macOS");
    }

    public int getClassId() {
        throw new UnsupportedOperationException("Apple object detection is only supported on macOS");
    }

    public double getConfidence() {
        throw new UnsupportedOperationException("Apple object detection is only supported on macOS");
    }
}
