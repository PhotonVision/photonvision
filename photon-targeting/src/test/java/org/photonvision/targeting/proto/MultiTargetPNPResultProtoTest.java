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

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.photonvision.targeting.MultiTargetPNPResult;
import org.photonvision.targeting.PnpResult;

public class MultiTargetPNPResultProtoTest {
    @Test
    public void protobufTest() {
        var result = new MultiTargetPNPResult();
        var serializedResult = MultiTargetPNPResult.proto.createMessage();
        MultiTargetPNPResult.proto.pack(serializedResult, result);
        var unpackedResult = MultiTargetPNPResult.proto.unpack(serializedResult);
        assertEquals(result, unpackedResult);

        result =
                new MultiTargetPNPResult(
                        new PnpResult(
                                new Transform3d(new Translation3d(1, 2, 3), new Rotation3d(1, 2, 3)), 0.1),
                        List.of((short) 1, (short) 2, (short) 3));
        serializedResult = MultiTargetPNPResult.proto.createMessage();
        MultiTargetPNPResult.proto.pack(serializedResult, result);
        unpackedResult = MultiTargetPNPResult.proto.unpack(serializedResult);
        assertEquals(result, unpackedResult);
    }
}
