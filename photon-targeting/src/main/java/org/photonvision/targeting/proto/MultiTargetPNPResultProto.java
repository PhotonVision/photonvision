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
import org.photonvision.proto.Photon.ProtobufMultiTargetPNPResult;
import org.photonvision.targeting.MultiTargetPNPResult;
import org.photonvision.targeting.PnpResult;
import us.hebi.quickbuf.Descriptors.Descriptor;
import us.hebi.quickbuf.RepeatedInt;

public class MultiTargetPNPResultProto
        implements Protobuf<MultiTargetPNPResult, ProtobufMultiTargetPNPResult> {
    @Override
    public Class<MultiTargetPNPResult> getTypeClass() {
        return MultiTargetPNPResult.class;
    }

    @Override
    public Descriptor getDescriptor() {
        return ProtobufMultiTargetPNPResult.getDescriptor();
    }

    @Override
    public ProtobufMultiTargetPNPResult createMessage() {
        return ProtobufMultiTargetPNPResult.newInstance();
    }

    @Override
    public MultiTargetPNPResult unpack(ProtobufMultiTargetPNPResult msg) {
        ArrayList<Short> fidIdsUsed = new ArrayList<>(msg.getFiducialIdsUsed().length());
        for (var packedFidId : msg.getFiducialIdsUsed()) {
            fidIdsUsed.add(packedFidId.shortValue());
        }

        return new MultiTargetPNPResult(PnpResult.proto.unpack(msg.getEstimatedPose()), fidIdsUsed);
    }

    @Override
    public void pack(ProtobufMultiTargetPNPResult msg, MultiTargetPNPResult value) {
        PnpResult.proto.pack(msg.getMutableEstimatedPose(), value.estimatedPose);

        RepeatedInt idsUsed = msg.getMutableFiducialIdsUsed().reserve(value.fiducialIDsUsed.size());
        for (int i = 0; i < value.fiducialIDsUsed.size(); i++) {
            idsUsed.add(value.fiducialIDsUsed.get(i));
        }
    }
}
