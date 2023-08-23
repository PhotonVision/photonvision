package org.photonvision.targeting;

import java.util.ArrayList;
import java.util.List;
import org.photonvision.common.dataflow.structures.Packet;

public class MultiTargetPNPResults {
    // pnpresult + 32 possible targets (arbitrary upper limit that should never be hit, ideally)
    private static final int MAX_IDS = 32;
    public static final int PACK_SIZE_BYTES = PNPResults.PACK_SIZE_BYTES + Byte.BYTES * MAX_IDS;

    public PNPResults estimatedPose = new PNPResults();
    public List<Integer> fiducialIDsUsed = List.of();

    public MultiTargetPNPResults() {}

    public MultiTargetPNPResults(PNPResults results, ArrayList<Integer> ids) {
        estimatedPose = results;
        fiducialIDsUsed = ids;
    }

    public static MultiTargetPNPResults createFromPacket(Packet packet) {
        var results = PNPResults.createFromPacket(packet);
        var ids = new ArrayList<Integer>(MAX_IDS);
        for (int i = 0; i < MAX_IDS; i++) {
            ids.add((int) packet.decodeByte());
        }
        return new MultiTargetPNPResults(results, ids);
    }

    public void populatePacket(Packet packet) {
        estimatedPose.populatePacket(packet);
        for (int i = 0; i < MAX_IDS; i++) {
            if (i < fiducialIDsUsed.size()) {
                packet.encode(fiducialIDsUsed.get(i).byteValue());
            } else {
                packet.encode(Byte.MIN_VALUE);
            }
        }
    }
}
