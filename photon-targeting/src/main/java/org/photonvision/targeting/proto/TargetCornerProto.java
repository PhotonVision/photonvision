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
