package org.photonvision;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.function.BooleanSupplier;
import org.photonvision.targeting.PhotonPipelineResult;

public class UnitTestUtils {
    static void waitForCondition(String name, BooleanSupplier condition) {
        // wait up to 1 second for satisfaction
        for (int i = 0; i < 100; i++) {
            if (condition.getAsBoolean()) {
                System.out.println(name + " satisfied on iteration " + i);
                return;
            }

            try {
                Thread.sleep(60);
            } catch (InterruptedException e) {
                e.printStackTrace();
                fail(e);
            }
        }
        throw new RuntimeException(name + " was never satisfied");
    }

    static PhotonPipelineResult waitForSequenceNumber(PhotonCamera camera, int seq) {
        assertTrue(camera.heartbeatEntry.getTopic().getHandle() != 0);

        System.out.println(
                "Waiting for seq=" + seq + " on " + camera.heartbeatEntry.getTopic().getName());
        // wait up to 1 second for a new result
        for (int i = 0; i < 100; i++) {
            var res = camera.getLatestResult();
            System.out.println(res);
            if (res.metadata.sequenceID == seq) {
                return res;
            }

            try {
                Thread.sleep(60);
            } catch (InterruptedException e) {
                e.printStackTrace();
                fail(e);
            }
        }
        throw new RuntimeException("Never saw sequence number " + seq);
    }
}
