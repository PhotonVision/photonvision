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

package org.photonvision.targeting;

import edu.wpi.first.util.protobuf.ProtobufSerializable;
import java.util.Objects;
import org.photonvision.common.dataflow.structures.PacketSerde;
import org.photonvision.struct.TargetCornerSerde;
import org.photonvision.targeting.proto.TargetCornerProto;
import org.photonvision.targeting.serde.PhotonStructSerializable;

/**
 * Represents a point in an image at the corner of the minimum-area bounding rectangle, in pixels.
 * Origin at the top left, plus-x to the right, plus-y down.
 */
public class TargetCorner implements ProtobufSerializable, PhotonStructSerializable<TargetCorner> {
    public double x;
    public double y;

    public TargetCorner(double cx, double cy) {
        this.x = cx;
        this.y = cy;
    }

    public TargetCorner() {
        this(0, 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TargetCorner that = (TargetCorner) o;
        return Double.compare(that.x, x) == 0 && Double.compare(that.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ')';
    }

    public static final TargetCornerProto proto = new TargetCornerProto();
    public static final TargetCornerSerde photonStruct = new TargetCornerSerde();

    @Override
    public PacketSerde<TargetCorner> getSerde() {
        return photonStruct;
    }
}
