/*
 * MIT License
 *
 * Copyright (c) 2022 PhotonVision
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.photonvision;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;

/**
 * Describes the 3d model of a target.
 */
public class TargetModel {
    /**
     * Translations of this target's vertices relative to its pose. If this target is
     * spherical, this list has one translation with x == radius.
     */
    public final List<Translation3d> vertices;
    public final boolean isPlanar;
    public final boolean isSpherical;

    /**
     * Creates a rectangular, planar target model given the width and height.
     */
    public TargetModel(double widthMeters, double heightMeters) {
        this.vertices = List.of(
            // this order is relevant for AprilTag compatibility
            new Translation3d(0, -widthMeters/2.0, -heightMeters/2.0),
            new Translation3d(0, widthMeters/2.0, -heightMeters/2.0),
            new Translation3d(0, widthMeters/2.0, heightMeters/2.0),
            new Translation3d(0, -widthMeters/2.0, heightMeters/2.0)
        );
        this.isPlanar = true;
        this.isSpherical = false;
    }
    /**
     * Creates a spherical target model which has similar dimensions when viewed from any angle.
     * This model will only have one vertex which has x == radius.
     */
    public TargetModel(double diameterMeters) {
        this.vertices = List.of(new Translation3d(diameterMeters / 2.0, 0, 0));
        this.isPlanar = false;
        this.isSpherical = true;
    }
    /**
     * Creates a target model from arbitrary 3d vertices. Automatically determines if the given
     * vertices are planar. More than 2 vertices must be given.
     * 
     * @param vertices Translations representing the vertices of this target model relative to its
     *     pose.
     */
    public TargetModel(List<Translation3d> vertices) {
        this.isSpherical = false;
        if(vertices == null || vertices.size() <= 2) {
            vertices = new ArrayList<>();
            this.isPlanar = false;
        }
        else {
            boolean cornersPlanar = true;
            for(Translation3d corner : vertices) {
                if(corner.getX() != 0) cornersPlanar = false;
            }
            if(vertices.size() != 4 || !cornersPlanar) {
                throw new IllegalArgumentException(
                    String.format(
                        "Supplied target corners (%s) must total 4 and be planar (all X == 0).",
                        vertices.size()
                    )
                );
            };
            this.isPlanar = true;
        }
        this.vertices = vertices;
    }

    /**
     * This target's vertices offset from its field pose.
     * 
     * <p>Note: If this target is spherical, only one vertex radius meters in front of the
     * pose is returned. 
     */
    public List<Translation3d> getFieldVertices(Pose3d targetPose) {
        return vertices.stream()
            .map(t -> targetPose.plus(new Transform3d(t, new Rotation3d())).getTranslation())
            .collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(obj instanceof TargetModel) {
            var o = (TargetModel)obj;
            return vertices.equals(o.vertices) &&
                    isPlanar == o.isPlanar &&
                    isSpherical == o.isSpherical;
        }
        return false;
    }
}
