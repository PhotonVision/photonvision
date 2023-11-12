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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.photonvision.proto.PhotonTypes;
import us.hebi.quickbuf.RepeatedMessage;

public class TargetCornerTest {
    @Test
    public void protobufTest() {
        var corner = new TargetCorner(0, 1);
        var serializedCorner = TargetCorner.proto.createMessage();
        TargetCorner.proto.pack(serializedCorner, corner);
        var unpackedCorner = TargetCorner.proto.unpack(serializedCorner);
        assertEquals(corner, unpackedCorner);
    }

    @Test
    public void protobufListTest() {
        List<TargetCorner> corners = List.of();
        var serializedCorners =
                RepeatedMessage.newEmptyInstance(PhotonTypes.ProtobufTargetCorner.getFactory());
        TargetCorner.proto.pack(serializedCorners, corners);
        var unpackedCorners = TargetCorner.proto.unpack(serializedCorners);
        assertEquals(corners, unpackedCorners);

        corners = List.of(new TargetCorner(0, 1), new TargetCorner(1, 2));
        serializedCorners =
                RepeatedMessage.newEmptyInstance(PhotonTypes.ProtobufTargetCorner.getFactory());
        TargetCorner.proto.pack(serializedCorners, corners);
        unpackedCorners = TargetCorner.proto.unpack(serializedCorners);
        assertEquals(corners, unpackedCorners);
    }

    @Test
    public void equalityTest() {
        var a = new TargetCorner(0, 1);
        var b = new TargetCorner(0, 1);

        assertEquals(a, b);
    }

    @Test
    public void inequalityTest() {
        var a = new TargetCorner(0, 1);
        var b = new TargetCorner(2, 4);

        assertNotEquals(a, b);
    }
}
