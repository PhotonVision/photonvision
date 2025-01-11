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
import java.util.Optional;
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
    public ProtobufPhotonPipelineResult createMessage() {
        return ProtobufPhotonPipelineResult.newInstance();
    }

    @Override
    public PhotonPipelineResult unpack(ProtobufPhotonPipelineResult msg) {
        return new PhotonPipelineResult(
                msg.getSequenceId(),
                msg.getCaptureTimestampMicros(),
                msg.getNtPublishTimestampMicros(),
                msg.getTimeSinceLastPongMicros(),
                PhotonTrackedTarget.proto.unpack(msg.getTargets()),
                msg.hasMultiTargetResult()
                        ? Optional.of(MultiTargetPNPResult.proto.unpack(msg.getMultiTargetResult()))
                        : Optional.empty());
    }

    @Override
    public void pack(ProtobufPhotonPipelineResult msg, PhotonPipelineResult value) {
        PhotonTrackedTarget.proto.pack(msg.getMutableTargets(), value.getTargets());

        if (value.getMultiTagResult().isPresent()) {
            MultiTargetPNPResult.proto.pack(
                    msg.getMutableMultiTargetResult(), value.getMultiTagResult().get());
        } else {
            msg.clearMultiTargetResult();
        }

        msg.setSequenceId(value.metadata.getSequenceID());
        msg.setCaptureTimestampMicros(value.metadata.getCaptureTimestampMicros());
        msg.setNtPublishTimestampMicros(value.metadata.getPublishTimestampMicros());
        msg.setTimeSinceLastPongMicros(value.metadata.timeSinceLastPong);
    }
}
