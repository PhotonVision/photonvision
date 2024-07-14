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
    public Protobuf<?, ?>[] getNested() {
        return new Protobuf<?, ?>[] {PnpResult.proto};
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
