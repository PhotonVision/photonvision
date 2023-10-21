package org.photonvision.targeting;

import org.photonvision.proto.PhotonTypes.PhotonPipelineResult;

import edu.wpi.first.util.protobuf.Protobuf;
import us.hebi.quickbuf.Descriptors.Descriptor;

public class PhotonPipelineResultProto implements Protobuf<PhotonPipelineResult, PhotonPipelineResult> {

    @Override
    public Class<PhotonPipelineResult> getTypeClass() {
        return PhotonPipelineResult.class;
    }

    @Override
    public Descriptor getDescriptor() {
        return PhotonPipelineResult.getDescriptor();
    }

    @Override
    public PhotonPipelineResult createMessage() {
        return PhotonPipelineResult.newInstance();
    }

    @Override
    public PhotonPipelineResult unpack(PhotonPipelineResult msg) {
        return msg;
    }

    @Override
    public void pack(PhotonPipelineResult msg, PhotonPipelineResult value) {
        msg.copyFrom(value);
    }

    public static final PhotonPipelineResultProto proto = new PhotonPipelineResultProto();
}
