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

import edu.wpi.first.math.MatBuilder;
import edu.wpi.first.math.Nat;
import org.junit.jupiter.api.Test;
import org.photonvision.common.dataflow.structures.Packet;

public class PhotonFramePropsTest {
    @Test
    public void testEncodeDecode() {
        PhotonFrameProps p =
                new PhotonFrameProps(
                        320,
                        240,
                        90,
                        new MatBuilder<>(Nat.N3(), Nat.N3()).fill(1, 2, 3, 4, 5, 6, 7, 8, 9),
                        new MatBuilder<>(Nat.N5(), Nat.N1()).fill(1, 2, 3, 4, 5));

        var packet = new Packet(PhotonFrameProps.PACKED_SIZE_BYTES);
        p.populatePacket(packet);

        var decoded = PhotonFrameProps.createFromPacket(packet);
        assertEquals(p, decoded);
    }

    @Test
    public void testInResult() {
        Packet packet;
        PhotonPipelineResult result;
        {
            PhotonFrameProps p =
                    new PhotonFrameProps(
                            320,
                            240,
                            90,
                            new MatBuilder<>(Nat.N3(), Nat.N3()).fill(1, 2, 3, 4, 5, 6, 7, 8, 9),
                            new MatBuilder<>(Nat.N5(), Nat.N1()).fill(1, 2, 3, 4, 5));

            result = new PhotonPipelineResult();
            result.setFrameProperties(p);

            packet = new Packet(result.getPacketSize());
            result.populatePacket(packet);
        }

        {
            var packet2 = new Packet(packet.getData());
            var decoded = new PhotonPipelineResult();
            decoded.createFromPacket(packet2);
            assertEquals(result, decoded);
        }
    }
}
