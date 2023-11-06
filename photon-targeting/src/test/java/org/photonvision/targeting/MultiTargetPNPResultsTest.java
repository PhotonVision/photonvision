package org.photonvision.targeting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import java.util.List;
import org.junit.jupiter.api.Test;

public class MultiTargetPNPResultsTest {
    @Test
    public void protobufTest() {
        var result = new MultiTargetPNPResults();
        var serializedResult = MultiTargetPNPResults.proto.createMessage();
        MultiTargetPNPResults.proto.pack(serializedResult, result);
        var unpackedResult = MultiTargetPNPResults.proto.unpack(serializedResult);
        assertEquals(result, unpackedResult);

        result =
                new MultiTargetPNPResults(
                        new PNPResults(
                                new Transform3d(new Translation3d(1, 2, 3), new Rotation3d(1, 2, 3)), 0.1),
                        List.of(1, 2, 3));
        serializedResult = MultiTargetPNPResults.proto.createMessage();
        MultiTargetPNPResults.proto.pack(serializedResult, result);
        unpackedResult = MultiTargetPNPResults.proto.unpack(serializedResult);
        assertEquals(result, unpackedResult);
    }
}
