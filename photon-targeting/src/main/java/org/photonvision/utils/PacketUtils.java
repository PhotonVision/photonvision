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

import edu.wpi.first.math.geometry.Quaternion;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import org.photonvision.common.dataflow.structures.Packet;

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
