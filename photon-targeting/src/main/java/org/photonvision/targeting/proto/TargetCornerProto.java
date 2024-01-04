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

package org.photonvision.targeting.proto;

import edu.wpi.first.util.protobuf.Protobuf;
import java.util.ArrayList;
import java.util.List;
import org.photonvision.proto.Photon.ProtobufTargetCorner;
import org.photonvision.targeting.TargetCorner;
import us.hebi.quickbuf.Descriptors.Descriptor;
import us.hebi.quickbuf.RepeatedMessage;

public class TargetCornerProto implements Protobuf<TargetCorner, ProtobufTargetCorner> {
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
        for (ProtobufTargetCorner corner : msg) {
            corners.add(unpack(corner));
        }
        return corners;
    }

    @Override
    public void pack(ProtobufTargetCorner msg, TargetCorner value) {
        msg.setX(value.x).setY(value.y);
    }

    public void pack(RepeatedMessage<ProtobufTargetCorner> msg, List<TargetCorner> value) {
        var corners = msg.reserve(value.size());
        for (TargetCorner targetCorner : value) {
            var corner = corners.next();
            pack(corner, targetCorner);
        }
    }
}
