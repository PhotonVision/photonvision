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
