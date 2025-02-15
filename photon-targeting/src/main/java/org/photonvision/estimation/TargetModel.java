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

package org.photonvision.estimation;

import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.util.Units;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Describes the 3d model of a target. */
public class TargetModel {
    /**
     * Translations of this target's vertices relative to its pose. Rectangular and spherical targets
     * will have four vertices. See their respective constructors for more info.
     */
    public final List<Translation3d> vertices;

    public final boolean isPlanar;
    public final boolean isSpherical;

    public static final TargetModel kAprilTag16h5 =
            new TargetModel(Units.inchesToMeters(6), Units.inchesToMeters(6));
    public static final TargetModel kAprilTag36h11 =
            new TargetModel(Units.inchesToMeters(6.5), Units.inchesToMeters(6.5));

    /**
     * Creates a rectangular, planar target model given the width and height. The model has four
     * vertices:
     *
     * <ul>
     *   <li>Point 0: [0, -width/2, -height/2]
     *   <li>Point 1: [0, width/2, -height/2]
     *   <li>Point 2: [0, width/2, height/2]
     *   <li>Point 3: [0, -width/2, height/2]
     * </ul>
     */
    public TargetModel(double widthMeters, double heightMeters) {
        this.vertices =
                List.of(
                        // this order is relevant for AprilTag compatibility
                        new Translation3d(0, -widthMeters / 2.0, -heightMeters / 2.0),
                        new Translation3d(0, widthMeters / 2.0, -heightMeters / 2.0),
                        new Translation3d(0, widthMeters / 2.0, heightMeters / 2.0),
                        new Translation3d(0, -widthMeters / 2.0, heightMeters / 2.0));
        this.isPlanar = true;
        this.isSpherical = false;
    }

    /**
     * Creates a cuboid target model given the length, width, height. The model has eight vertices:
     *
     * <ul>
     *   <li>Point 0: [length/2, -width/2, -height/2]
     *   <li>Point 1: [length/2, width/2, -height/2]
     *   <li>Point 2: [length/2, width/2, height/2]
     *   <li>Point 3: [length/2, -width/2, height/2]
     *   <li>Point 4: [-length/2, -width/2, height/2]
     *   <li>Point 5: [-length/2, width/2, height/2]
     *   <li>Point 6: [-length/2, width/2, -height/2]
     *   <li>Point 7: [-length/2, -width/2, -height/2]
     * </ul>
     */
    public TargetModel(double lengthMeters, double widthMeters, double heightMeters) {
        this(
                List.of(
                        new Translation3d(lengthMeters / 2.0, -widthMeters / 2.0, -heightMeters / 2.0),
                        new Translation3d(lengthMeters / 2.0, widthMeters / 2.0, -heightMeters / 2.0),
                        new Translation3d(lengthMeters / 2.0, widthMeters / 2.0, heightMeters / 2.0),
                        new Translation3d(lengthMeters / 2.0, -widthMeters / 2.0, heightMeters / 2.0),
                        new Translation3d(-lengthMeters / 2.0, -widthMeters / 2.0, heightMeters / 2.0),
                        new Translation3d(-lengthMeters / 2.0, widthMeters / 2.0, heightMeters / 2.0),
                        new Translation3d(-lengthMeters / 2.0, widthMeters / 2.0, -heightMeters / 2.0),
                        new Translation3d(-lengthMeters / 2.0, -widthMeters / 2.0, -heightMeters / 2.0)));
    }

    /**
     * Creates a spherical target model which has similar dimensions regardless of its rotation. This
     * model has four vertices:
     *
     * <ul>
     *   <li>Point 0: [0, -radius, 0]
     *   <li>Point 1: [0, 0, -radius]
     *   <li>Point 2: [0, radius, 0]
     *   <li>Point 3: [0, 0, radius]
     * </ul>
     *
     * <i>Q: Why these vertices?</i> A: This target should be oriented to the camera every frame, much
     * like a sprite/decal, and these vertices represent the ellipse vertices (maxima). These vertices
     * are used for drawing the image of this sphere, but do not match the corners that will be
     * published by photonvision.
     */
    public TargetModel(double diameterMeters) {
        double radius = diameterMeters / 2.0;
        this.vertices =
                List.of(
                        new Translation3d(0, -radius, 0),
                        new Translation3d(0, 0, -radius),
                        new Translation3d(0, radius, 0),
                        new Translation3d(0, 0, radius));
        this.isPlanar = false;
        this.isSpherical = true;
    }

    /**
     * Creates a target model from arbitrary 3d vertices. Automatically determines if the given
     * vertices are planar(x == 0). More than 2 vertices must be given. If this is a planar model, the
     * vertices should define a non-intersecting contour.
     *
     * @param vertices Translations representing the vertices of this target model relative to its
     *     pose.
     */
    public TargetModel(List<Translation3d> vertices) {
        this.isSpherical = false;
        if (vertices == null || vertices.size() <= 2) {
            vertices = new ArrayList<>();
            this.isPlanar = false;
        } else {
            boolean cornersPlanar = true;
            for (Translation3d corner : vertices) {
                if (corner.getX() != 0) cornersPlanar = false;
            }
            this.isPlanar = cornersPlanar;
        }
        this.vertices = vertices;
    }

    /**
     * This target's vertices offset from its field pose.
     *
     * <p>Note: If this target is spherical, use {@link #getOrientedPose(Translation3d,
     * Translation3d)} with this method.
     */
    public List<Translation3d> getFieldVertices(Pose3d targetPose) {
        var basisChange = new RotTrlTransform3d(targetPose.getRotation(), targetPose.getTranslation());
        return vertices.stream().map(basisChange::apply).collect(Collectors.toList());
    }

    /**
     * Returns a Pose3d with the given target translation oriented (with its relative x-axis aligned)
     * to the camera translation. This is used for spherical targets which should not have their
     * projection change regardless of their own rotation.
     *
     * @param tgtTrl This target's translation
     * @param cameraTrl Camera's translation
     * @return This target's pose oriented to the camera
     */
    public static Pose3d getOrientedPose(Translation3d tgtTrl, Translation3d cameraTrl) {
        var relCam = cameraTrl.minus(tgtTrl);
        var orientToCam =
                new Rotation3d(
                        0,
                        new Rotation2d(Math.hypot(relCam.getX(), relCam.getY()), -relCam.getZ()).getRadians(),
                        new Rotation2d(relCam.getX(), relCam.getY()).getRadians());
        return new Pose3d(tgtTrl, orientToCam);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj
                && obj instanceof TargetModel o
                && vertices.equals(o.vertices)
                && isPlanar == o.isPlanar
                && isSpherical == o.isSpherical;
    }
}
