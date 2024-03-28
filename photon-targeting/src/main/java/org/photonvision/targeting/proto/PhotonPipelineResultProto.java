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
import org.photonvision.proto.Photon.ProtobufPhotonPipelineResult;
import org.photonvision.targeting.MultiTargetPNPResult;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;
import us.hebi.quickbuf.Descriptors.Descriptor;

public class PhotonPipelineResultProto
        implements Protobuf<PhotonPipelineResult, ProtobufPhotonPipelineResult> {
    @Override
    public Class<PhotonPipelineResult> getTypeClass() {
        return PhotonPipelineResult.class;
    }

    @Override
    public Descriptor getDescriptor() {
        return ProtobufPhotonPipelineResult.getDescriptor();
    }

    @Override
    public Protobuf<?, ?>[] getNested() {
        return new Protobuf<?, ?>[] {PhotonTrackedTarget.proto, MultiTargetPNPResult.proto};
    }

    @Override
    public ProtobufPhotonPipelineResult createMessage() {
        return ProtobufPhotonPipelineResult.newInstance();
    }

    @Override
    public PhotonPipelineResult unpack(ProtobufPhotonPipelineResult msg) {
        return new PhotonPipelineResult(
                msg.getSequenceId(),
                msg.getCaptureTimestampMicros(),
                msg.getNtPublishTimestampMicros(),
                PhotonTrackedTarget.proto.unpack(msg.getTargets()),
                MultiTargetPNPResult.proto.unpack(msg.getMultiTargetResult()));
    }

    @Override
    public void pack(ProtobufPhotonPipelineResult msg, PhotonPipelineResult value) {
        PhotonTrackedTarget.proto.pack(msg.getMutableTargets(), value.getTargets());
        MultiTargetPNPResult.proto.pack(msg.getMutableMultiTargetResult(), value.getMultiTagResult());

        msg.setSequenceId(value.getSequenceID());
        msg.setCaptureTimestampMicros(value.getCaptureTimestampMicros());
        msg.setNtPublishTimestampMicros(value.getPublishTimestampMicros());
    }
}
