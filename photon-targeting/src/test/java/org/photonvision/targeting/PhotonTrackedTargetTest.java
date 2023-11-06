package org.photonvision.targeting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import java.util.List;
import org.junit.jupiter.api.Test;

public class PhotonTrackedTargetTest {
    @Test
    public void protobufTest() {
        var target = new PhotonTrackedTarget();
        var serializedTarget = PhotonTrackedTarget.proto.createMessage();
        PhotonTrackedTarget.proto.pack(serializedTarget, target);
        var unpackedTarget = PhotonTrackedTarget.proto.unpack(serializedTarget);
        assertEquals(target, unpackedTarget);

        target =
                new PhotonTrackedTarget(
                        3.0,
                        4.0,
                        9.0,
                        -5.0,
                        -1,
                        new Transform3d(new Translation3d(), new Rotation3d()),
                        new Transform3d(new Translation3d(), new Rotation3d()),
                        0.25,
                        List.of(
                                new TargetCorner(1, 2),
                                new TargetCorner(3, 4),
                                new TargetCorner(5, 6),
                                new TargetCorner(7, 8)),
                        List.of(
                                new TargetCorner(1, 2),
                                new TargetCorner(3, 4),
                                new TargetCorner(5, 6),
                                new TargetCorner(7, 8)));
        serializedTarget = PhotonTrackedTarget.proto.createMessage();
        PhotonTrackedTarget.proto.pack(serializedTarget, target);
        unpackedTarget = PhotonTrackedTarget.proto.unpack(serializedTarget);
        assertEquals(target, unpackedTarget);
    }
}
