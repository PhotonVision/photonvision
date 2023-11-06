// /*
//  * Copyright (C) Photon Vision.
//  *
//  * This program is free software: you can redistribute it and/or modify
//  * it under the terms of the GNU General Public License as published by
//  * the Free Software Foundation, either version 3 of the License, or
//  * (at your option) any later version.
//  *
//  * This program is distributed in the hope that it will be useful,
//  * but WITHOUT ANY WARRANTY; without even the implied warranty of
//  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  * GNU General Public License for more details.
//  *
//  * You should have received a copy of the GNU General Public License
//  * along with this program.  If not, see <https://www.gnu.org/licenses/>.
//  */

package org.photonvision.targeting;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import edu.wpi.first.util.protobuf.Protobuf;
import org.photonvision.proto.PhotonTypes.ProtobufTargetCorner;
import us.hebi.quickbuf.Descriptors.Descriptor;
import us.hebi.quickbuf.RepeatedMessage;

/**
 * Represents a point in an image at the corner of the minimum-area bounding rectangle, in pixels.
 * Origin at the top left, plus-x to the right, plus-y down.
 */
public class TargetCorner {
    public final double x;
    public final double y;

    public TargetCorner(double cx, double cy) {
        this.x = cx;
        this.y = cy;
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

    public static final class AProto implements Protobuf<TargetCorner, ProtobufTargetCorner> {
        @Override
        public Class<TargetCorner> getTypeClass() {
            return TargetCorner.class;
        }

        @Override
        public Descriptor getDescriptor() {
            return ProtobufTargetCorner.getDescriptor();
        }

        @Override
        public ProtobufTargetCorner createMessage() {
            return ProtobufTargetCorner.newInstance();
        }

        @Override
        public TargetCorner unpack(ProtobufTargetCorner msg) {
            return new TargetCorner(msg.getX(), msg.getY());
        }

        public List<TargetCorner> unpack(RepeatedMessage<ProtobufTargetCorner> msg) {
            ArrayList<TargetCorner> corners = new ArrayList<>(msg.length());
            for(ProtobufTargetCorner corner : msg) {
                corners.add(unpack(corner));
            }
            return corners;
        }

        @Override
        public void pack(ProtobufTargetCorner msg, TargetCorner value) {
            msg.setX(value.x).setY(value.y);
        }

        public void pack(ProtobufTargetCorner[] buffer, List<TargetCorner> corners) {
            for(int i = 0; i < corners.size(); i++) {
                var protoCorner = createMessage();
                pack(protoCorner, corners.get(i));
                buffer[i] = protoCorner;
            }
        }
    }

    public static final AProto proto = new AProto();
}
