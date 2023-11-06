package org.photonvision.targeting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TargetCornerTest {
    @Test
    public void protobufTest() {
        var corner = new TargetCorner(0, 1);
        var serializedCorner = TargetCorner.proto.createMessage();
        TargetCorner.proto.pack(serializedCorner, corner);
        var unpackedCorner = TargetCorner.proto.unpack(serializedCorner);
        assertEquals(corner, unpackedCorner);
    }
}
