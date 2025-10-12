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

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import org.junit.jupiter.api.Test;
import org.photonvision.targeting.PnpResult;

public class PNPResultProtoTest {
    @Test
    public void protobufTest() {
        var pnpRes = new PnpResult();
        var serializedPNPRes = PnpResult.proto.createMessage();
        PnpResult.proto.pack(serializedPNPRes, pnpRes);
        var unpackedPNPRes = PnpResult.proto.unpack(serializedPNPRes);
        assertEquals(pnpRes, unpackedPNPRes);

        pnpRes = new PnpResult(new Transform3d(1, 2, 3, new Rotation3d(1, 2, 3)), 0.1);
        serializedPNPRes = PnpResult.proto.createMessage();
        PnpResult.proto.pack(serializedPNPRes, pnpRes);
        unpackedPNPRes = PnpResult.proto.unpack(serializedPNPRes);
        assertEquals(pnpRes, unpackedPNPRes);
    }
}
