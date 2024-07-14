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
    public Protobuf<?, ?>[] getNested() {
        return new Protobuf<?, ?>[] {Transform3d.proto};
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
