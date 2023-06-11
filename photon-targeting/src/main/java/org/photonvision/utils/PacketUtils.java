package org.photonvision.utils;

import org.photonvision.common.dataflow.structures.Packet;

import edu.wpi.first.math.geometry.Quaternion;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;

public class PacketUtils {
    public static Transform3d decodeTransform(Packet packet) {
        double x = packet.decodeDouble();
        double y = packet.decodeDouble();
        double z = packet.decodeDouble();
        var translation = new Translation3d(x, y, z);
        double w = packet.decodeDouble();
        x = packet.decodeDouble();
        y = packet.decodeDouble();
        z = packet.decodeDouble();
        var rotation = new Rotation3d(new Quaternion(w, x, y, z));
        return new Transform3d(translation, rotation);
    }

    public static void encodeTransform(Packet packet, Transform3d transform) {
        packet.encode(transform.getTranslation().getX());
        packet.encode(transform.getTranslation().getY());
        packet.encode(transform.getTranslation().getZ());
        packet.encode(transform.getRotation().getQuaternion().getW());
        packet.encode(transform.getRotation().getQuaternion().getX());
        packet.encode(transform.getRotation().getQuaternion().getY());
        packet.encode(transform.getRotation().getQuaternion().getZ());
    }

}
