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

import org.photonvision.common.dataflow.structures.Packet;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.wpilib.math.geometry.*;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Quaternion;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.geometry.Translation3d;

@SuppressWarnings("doclint")
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
        packet.encodeDouble(rotation.getRadians());
    }

    public static Rotation2d unpackRotation2d(Packet packet) {
        return new Rotation2d(packet.decodeDouble());
    }

    public static void packQuaternion(Packet packet, Quaternion quaternion) {
        packet.encodeDouble(quaternion.getW());
        packet.encodeDouble(quaternion.getX());
        packet.encodeDouble(quaternion.getY());
        packet.encodeDouble(quaternion.getZ());
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
        packet.encodeDouble(translation.getX());
        packet.encodeDouble(translation.getY());
    }

    public static Translation2d unpackTranslation2d(Packet packet) {
        return new Translation2d(packet.decodeDouble(), packet.decodeDouble());
    }

    public static void packTranslation3d(Packet packet, Translation3d translation) {
        packet.encodeDouble(translation.getX());
        packet.encodeDouble(translation.getY());
        packet.encodeDouble(translation.getZ());
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

    public static <T> void packList(
            Packet packet, List<T> data, BiConsumer<Packet, T> packer) {
        byte size = (byte) data.size();
        if (data.size() > Byte.MAX_VALUE) {
            throw new RuntimeException("Array too long! Got " + size);
        }

        // length byte
        packet.encodeByte(size);

        for (var f : data) {
            packer.accept(packet,f);
        }
    }

    public static <T> List<T> unpackList(Packet packet, Function<Packet, T> unpacker) {
        byte length = packet.decodeByte();

        var ret = new ArrayList<T>();
        ret.ensureCapacity(length);

        for (int i = 0; i < length; i++) {
            ret.add(unpacker.apply(packet));
        }

        return ret;
    }

    public static <T> void packOptional(
            Packet packet, Optional<T> optional, BiConsumer<Packet, T> packer) {
        if (optional.isPresent()) {
            packet.encodeBoolean(true);
            packer.accept(packet, optional.get());
        } else {
            packet.encodeBoolean(false);
        }
    }

    public static <T> Optional<T> unpackOptional(Packet packet, Function<Packet, T> unpacker) {
        boolean isPresent = packet.decodeBoolean();
        if (isPresent) {
            return Optional.of(unpacker.apply(packet));
        } else {
            return Optional.empty();
        }
    }
}
