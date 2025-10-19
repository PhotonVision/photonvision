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

import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.util.protobuf.Protobuf;
import org.photonvision.proto.Photon.ProtobufPNPResult;
import org.photonvision.targeting.PnpResult;
import us.hebi.quickbuf.Descriptors.Descriptor;

public class PNPResultProto implements Protobuf<PnpResult, ProtobufPNPResult> {
    @Override
    public Class<PnpResult> getTypeClass() {
        return PnpResult.class;
    }

    @Override
    public Descriptor getDescriptor() {
        return ProtobufPNPResult.getDescriptor();
    }

    @Override
    public ProtobufPNPResult createMessage() {
        return ProtobufPNPResult.newInstance();
    }

    @Override
    public PnpResult unpack(ProtobufPNPResult msg) {
        return new PnpResult(
                Transform3d.proto.unpack(msg.getBest()),
                Transform3d.proto.unpack(msg.getAlt()),
                msg.getAmbiguity(),
                msg.getBestReprojErr(),
                msg.getAltReprojErr());
    }

    @Override
    public void pack(ProtobufPNPResult msg, PnpResult value) {
        Transform3d.proto.pack(msg.getMutableBest(), value.best);
        Transform3d.proto.pack(msg.getMutableAlt(), value.alt);
        msg.setAmbiguity(value.ambiguity)
                .setBestReprojErr(value.bestReprojErr)
                .setAltReprojErr(value.altReprojErr);
    }
}
