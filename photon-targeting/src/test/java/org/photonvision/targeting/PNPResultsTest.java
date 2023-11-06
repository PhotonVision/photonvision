package org.photonvision.targeting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import org.junit.jupiter.api.Test;

public class PNPResultsTest {
    @Test
    public void protobufTest() {
        var pnpRes = new PNPResults();
        var serializedPNPRes = PNPResults.proto.createMessage();
        PNPResults.proto.pack(serializedPNPRes, pnpRes);
        var unpackedPNPRes = PNPResults.proto.unpack(serializedPNPRes);
        assertEquals(pnpRes, unpackedPNPRes);

        pnpRes = new PNPResults(new Transform3d(1, 2, 3, new Rotation3d(1, 2, 3)), 0.1);
        serializedPNPRes = PNPResults.proto.createMessage();
        PNPResults.proto.pack(serializedPNPRes, pnpRes);
        unpackedPNPRes = PNPResults.proto.unpack(serializedPNPRes);
        assertEquals(pnpRes, unpackedPNPRes);
    }
}
