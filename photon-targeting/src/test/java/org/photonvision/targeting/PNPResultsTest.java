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
