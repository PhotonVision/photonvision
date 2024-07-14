package org.photonvision.targeting.serde;

import java.util.ArrayList;
import org.photonvision.common.dataflow.structures.Packet;
import org.photonvision.common.dataflow.structures.PacketSerde;
import org.photonvision.targeting.MultiTargetPNPResult;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;

public final class APacketSerde implements PacketSerde<PhotonPipelineResult> {
    @Override
    public int getMaxByteSize() {
        // This uses dynamic packets so it doesn't matter
        return -1;
    }

    @Override
    public void pack(Packet packet, PhotonPipelineResult value) {
        packet.encode(value.getSequenceID());
        packet.encode(value.getCaptureTimestampMicros());
        packet.encode(value.getPublishTimestampMicros());

        packet.encode((byte) value.targets.size());
        for (var target : value.targets) PhotonTrackedTarget.serde.pack(packet, target);

        MultiTargetPNPResult.serde.pack(packet, value.getMultiTagResult());
    }

    @Override
    public PhotonPipelineResult unpack(Packet packet) {
        var seq = packet.decodeLong();
        var cap = packet.decodeLong();
        var pub = packet.decodeLong();
        var len = packet.decodeByte();
        var targets = new ArrayList<PhotonTrackedTarget>(len);
        for (int i = 0; i < len; i++) {
            targets.add(PhotonTrackedTarget.serde.unpack(packet));
        }
        var result = MultiTargetPNPResult.serde.unpack(packet);

        return new PhotonPipelineResult(seq, cap, pub, targets, result);
    }
}
