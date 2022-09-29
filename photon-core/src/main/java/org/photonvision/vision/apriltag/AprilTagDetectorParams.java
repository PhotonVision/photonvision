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

import java.util.Objects;

public class AprilTagDetectorParams {
    public static AprilTagDetectorParams DEFAULT_36H11 =
            new AprilTagDetectorParams(AprilTagFamily.kTag36h11, 1.0, 0.0, 4, false, false);

    public final AprilTagFamily tagFamily;
    public final double decimate;
    public final double blur;
    public final int threads;
    public final boolean debug;
    public final boolean refineEdges;

    public AprilTagDetectorParams(
            AprilTagFamily tagFamily,
            double decimate,
            double blur,
            int threads,
            boolean debug,
            boolean refineEdges) {
        this.tagFamily = tagFamily;
        this.decimate = decimate;
        this.blur = blur;
        this.threads = threads;
        this.debug = debug;
        this.refineEdges = refineEdges;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AprilTagDetectorParams that = (AprilTagDetectorParams) o;
        return Objects.equals(tagFamily, that.tagFamily)
                && Double.compare(decimate, that.decimate) == 0
                && Double.compare(blur, that.blur) == 0
                && threads == that.threads
                && debug == that.debug
                && refineEdges == that.refineEdges;
    }

    @Override
    public String toString() {
        return "AprilTagDetectorParams{"
                + "tagFamily="
                + tagFamily.getNativeName()
                + ", decimate="
                + decimate
                + ", blur="
                + blur
                + ", threads="
                + threads
                + ", debug="
                + debug
                + ", refineEdges="
                + refineEdges
                + '}';
    }
}
