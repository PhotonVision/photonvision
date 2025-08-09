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

package org.photonvision.utils;

import edu.wpi.first.math.geometry.*;
import org.photonvision.common.dataflow.structures.Packet;

public class PacketUtils {
    public static final int ROTATION2D_BYTE_SIZE = Double.BYTES;
    public static final int QUATERNION_BYTE_SIZE = Double.BYTES * 4;
    public static final int ROTATION3D_BYTE_SIZE = QUATERNION_BYTE_SIZE;
    public static final int TRANSLATION2D_BYTE_SIZE = Double.BYTES * 2;
    public static final int TRANSLATION3D_BYTE_SIZE = Double.BYTES * 3;
    public static final int TRANSFORM2D_BYTE_SIZE = TRANSLATION2D_BYTE_SIZE + ROTATION2D_BYTE_SIZE;
    public static final int TRANSFORM3D_BYTE_SIZE = TRANSLATION3D_BYTE_SIZE + ROTATION3D_BYTE_SIZE;
    public static final int POSE2D_BYTE_SIZE = TRANSLATION2D_BYTE_SIZE + ROTATION2D_BYTE_SIZE;
    public static final int POSE3D_BYTE_SIZE = TRANSLATION3D_BYTE_SIZE + ROTATION3D_BYTE_SIZE;

    public static void packRotation2d(Packet packet, Rotation2d rotation) {
        packet.encode(rotation.getRadians());
    }

    public static Rotation2d unpackRotation2d(Packet packet) {
        return new Rotation2d(packet.decodeDouble());
    }

    public static void packQuaternion(Packet packet, Quaternion quaternion) {
        packet.encode(quaternion.getW());
        packet.encode(quaternion.getX());
        packet.encode(quaternion.getY());
        packet.encode(quaternion.getZ());
    }

    public static Quaternion unpackQuaternion(Packet packet) {
        return new Quaternion(
                packet.decodeDouble(), packet.decodeDouble(), packet.decodeDouble(), packet.decodeDouble());
    }

    public static void packRotation3d(Packet packet, Rotation3d rotation) {
        packQuaternion(packet, rotation.getQuaternion());
    }

    public static Rotation3d unpackRotation3d(Packet packet) {
        return new Rotation3d(unpackQuaternion(packet));
    }

    public static void packTranslation2d(Packet packet, Translation2d translation) {
        packet.encode(translation.getX());
        packet.encode(translation.getY());
    }

    public static Translation2d unpackTranslation2d(Packet packet) {
        return new Translation2d(packet.decodeDouble(), packet.decodeDouble());
    }

    public static void packTranslation3d(Packet packet, Translation3d translation) {
        packet.encode(translation.getX());
        packet.encode(translation.getY());
        packet.encode(translation.getZ());
    }

    public static Translation3d unpackTranslation3d(Packet packet) {
        return new Translation3d(packet.decodeDouble(), packet.decodeDouble(), packet.decodeDouble());
    }

    public static void packTransform2d(Packet packet, Transform2d transform) {
        packTranslation2d(packet, transform.getTranslation());
        packRotation2d(packet, transform.getRotation());
    }

    public static Transform2d unpackTransform2d(Packet packet) {
        return new Transform2d(unpackTranslation2d(packet), unpackRotation2d(packet));
    }

    public static void packTransform3d(Packet packet, Transform3d transform) {
        packTranslation3d(packet, transform.getTranslation());
        packRotation3d(packet, transform.getRotation());
    }

    public static Transform3d unpackTransform3d(Packet packet) {
        return new Transform3d(unpackTranslation3d(packet), unpackRotation3d(packet));
    }

    public static void packPose2d(Packet packet, Pose2d pose) {
        packTranslation2d(packet, pose.getTranslation());
        packRotation2d(packet, pose.getRotation());
    }

    public static Pose2d unpackPose2d(Packet packet) {
        return new Pose2d(unpackTranslation2d(packet), unpackRotation2d(packet));
    }

    public static void packPose3d(Packet packet, Pose3d pose) {
        packTranslation3d(packet, pose.getTranslation());
        packRotation3d(packet, pose.getRotation());
    }

    public static Pose3d unpackPose3d(Packet packet) {
        return new Pose3d(unpackTranslation3d(packet), unpackRotation3d(packet));
    }
}
