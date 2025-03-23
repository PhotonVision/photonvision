/*
 * MIT License
 *
 * Copyright (c) PhotonVision
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

package org.photonvision.estimation;

import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a transformation that first rotates a pose around the origin, and then translates it.
 */
public class RotTrlTransform3d {
    private final Translation3d trl;
    private final Rotation3d rot;

    /**
     * A rotation-translation transformation.
     *
     * <p>Applying this RotTrlTransform3d to poses will preserve their current origin-to-pose
     * transform as if the origin was transformed by these components instead.
     *
     * @param rot The rotation component
     * @param trl The translation component
     */
    public RotTrlTransform3d(Rotation3d rot, Translation3d trl) {
        this.rot = rot;
        this.trl = trl;
    }

    public RotTrlTransform3d(Pose3d initial, Pose3d last) {
        this.rot = last.getRotation().minus(initial.getRotation());
        this.trl = last.getTranslation().minus(initial.getTranslation().rotateBy(rot));
    }

    /**
     * Creates a rotation-translation transformation from a Transform3d.
     *
     * <p>Applying this RotTrlTransform3d to poses will preserve their current origin-to-pose
     * transform as if the origin was transformed by trf instead.
     *
     * @param trf The origin transformation
     */
    public RotTrlTransform3d(Transform3d trf) {
        this(trf.getRotation(), trf.getTranslation());
    }

    public RotTrlTransform3d() {
        this(new Rotation3d(), new Translation3d());
    }

    /**
     * The rotation-translation transformation that makes poses in the world consider this pose as the
     * new origin, or change the basis to this pose.
     *
     * @param pose The new origin
     */
    public static RotTrlTransform3d makeRelativeTo(Pose3d pose) {
        return new RotTrlTransform3d(pose.getRotation(), pose.getTranslation()).inverse();
    }

    /** The inverse of this transformation. Applying the inverse will "undo" this transformation. */
    public RotTrlTransform3d inverse() {
        var inverseRot = rot.unaryMinus();
        var inverseTrl = trl.rotateBy(inverseRot).unaryMinus();
        return new RotTrlTransform3d(inverseRot, inverseTrl);
    }

    /** This transformation as a Transform3d (as if of the origin) */
    public Transform3d getTransform() {
        return new Transform3d(trl, rot);
    }

    /** The translation component of this transformation */
    public Translation3d getTranslation() {
        return trl;
    }

    /** The rotation component of this transformation */
    public Rotation3d getRotation() {
        return rot;
    }

    public Translation3d apply(Translation3d trl) {
        return trl.rotateBy(rot).plus(this.trl);
    }

    public List<Translation3d> applyTrls(List<Translation3d> trls) {
        return trls.stream().map(this::apply).collect(Collectors.toList());
    }

    public Rotation3d apply(Rotation3d rot) {
        return rot.plus(this.rot);
    }

    public List<Rotation3d> applyRots(List<Rotation3d> rots) {
        return rots.stream().map(this::apply).collect(Collectors.toList());
    }

    public Pose3d apply(Pose3d pose) {
        return new Pose3d(apply(pose.getTranslation()), apply(pose.getRotation()));
    }

    public List<Pose3d> applyPoses(List<Pose3d> poses) {
        return poses.stream().map(this::apply).collect(Collectors.toList());
    }
}
