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
import edu.wpi.first.math.geometry.Quaternion;
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
    // TODO: removal awaiting wpilib Rotation3d performance improvements
    private double m_w;
    private double m_x;
    private double m_y;
    private double m_z;

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
        var quat = rot.getQuaternion();
        m_w = quat.getW();
        m_x = quat.getX();
        m_y = quat.getY();
        m_z = quat.getZ();
        this.trl = trl;
    }

    public RotTrlTransform3d(Pose3d initial, Pose3d last) {
        // this.rot = last.getRotation().minus(initial.getRotation());
        // this.trl = last.getTranslation().minus(initial.getTranslation().rotateBy(rot));

        var quat = initial.getRotation().getQuaternion();
        m_w = quat.getW();
        m_x = quat.getX();
        m_y = quat.getY();
        m_z = quat.getZ();
        this.rot = invrotate(last.getRotation());
        this.trl = last.getTranslation().minus(rotate(initial.getTranslation()));
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

    private Translation3d rotate(Translation3d otrl) {
        final var p = new Quaternion(0.0, otrl.getX(), otrl.getY(), otrl.getZ());
        final var qprime = times(times(p), new Quaternion(m_w, -m_x, -m_y, -m_z));
        return new Translation3d(qprime.getX(), qprime.getY(), qprime.getZ());
    }

    private Translation3d invrotate(Translation3d otrl) {
        m_x = -m_x;
        m_y = -m_y;
        m_z = -m_z;
        var result = rotate(otrl);
        m_x = -m_x;
        m_y = -m_y;
        m_z = -m_z;
        return result;
    }

    private Rotation3d rotate(Rotation3d orot) {
        return new Rotation3d(times(orot.getQuaternion()));
    }

    private Rotation3d invrotate(Rotation3d orot) {
        m_x = -m_x;
        m_y = -m_y;
        m_z = -m_z;
        var result = rotate(orot);
        m_x = -m_x;
        m_y = -m_y;
        m_z = -m_z;
        return result;
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
        // var inverseRot = rot.unaryMinus();
        // var inverseTrl = trl.rotateBy(inverseRot).unaryMinus();
        // return new RotTrlTransform3d(inverseRot, inverseTrl);

        var inverseTrl = invrotate(trl).unaryMinus();
        return new RotTrlTransform3d(new Rotation3d(new Quaternion(m_w, -m_x, -m_y, -m_z)), inverseTrl);
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
        // return trl.rotateBy(rot).plus(this.trl);
        return rotate(trl).plus(this.trl);
    }

    public List<Translation3d> applyTrls(List<Translation3d> trls) {
        return trls.stream().map(this::apply).collect(Collectors.toList());
    }

    public Rotation3d apply(Rotation3d rot) {
        return rotate(rot);
    }

    public List<Rotation3d> applyRots(List<Rotation3d> rots) {
        return rots.stream().map(this::apply).collect(Collectors.toList());
    }

    public Pose3d apply(Pose3d pose) {
        // return new Pose3d(pose.getTranslation().rotateBy(rot).plus(trl),
        // pose.getRotation().plus(rot));
        return new Pose3d(apply(pose.getTranslation()), apply(pose.getRotation()));
    }

    public List<Pose3d> applyPoses(List<Pose3d> poses) {
        return poses.stream().map(this::apply).collect(Collectors.toList());
    }

    // TODO: removal awaiting wpilib Rotation3d performance improvements
    private Quaternion times(Quaternion other) {
        final double o_w = other.getW();
        final double o_x = other.getX();
        final double o_y = other.getY();
        final double o_z = other.getZ();
        return times(m_w, m_x, m_y, m_z, o_w, o_x, o_y, o_z);
    }

    private static Quaternion times(Quaternion a, Quaternion b) {
        final double m_w = a.getW();
        final double m_x = a.getX();
        final double m_y = a.getY();
        final double m_z = a.getZ();
        final double o_w = b.getW();
        final double o_x = b.getX();
        final double o_y = b.getY();
        final double o_z = b.getZ();
        return times(m_w, m_x, m_y, m_z, o_w, o_x, o_y, o_z);
    }

    private static Quaternion times(
            double m_w,
            double m_x,
            double m_y,
            double m_z,
            double o_w,
            double o_x,
            double o_y,
            double o_z) {
        // https://en.wikipedia.org/wiki/Quaternion#Scalar_and_vector_parts

        // v₁ x v₂
        final double cross_x = m_y * o_z - o_y * m_z;
        final double cross_y = o_x * m_z - m_x * o_z;
        final double cross_z = m_x * o_y - o_x * m_y;

        // v = w₁v₂ + w₂v₁ + v₁ x v₂
        final double new_x = o_x * m_w + (m_x * o_w) + cross_x;
        final double new_y = o_y * m_w + (m_y * o_w) + cross_y;
        final double new_z = o_z * m_w + (m_z * o_w) + cross_z;

        return new Quaternion(m_w * o_w - (m_x * o_x + m_y * o_y + m_z * o_z), new_x, new_y, new_z);
    }
}
