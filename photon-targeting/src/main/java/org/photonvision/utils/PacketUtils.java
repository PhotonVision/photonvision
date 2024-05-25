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
