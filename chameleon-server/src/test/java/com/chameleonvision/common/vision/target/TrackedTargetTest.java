package com.chameleonvision.common.vision.target;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.chameleonvision.common.util.TestUtils;
import com.chameleonvision.common.util.numbers.DoubleCouple;
import com.chameleonvision.common.vision.opencv.Contour;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Size;

public class TrackedTargetTest {
    @BeforeEach
    public void Init() {
        TestUtils.loadLibraries();
    }

    @Test
    void axisTest() {
        Mat background = new Mat();

        MatOfPoint mat = new MatOfPoint();
        mat.fromList(
                List.of(
                        new Point(400, 298),
                        new Point(426.22, 298),
                        new Point(426.22, 302),
                        new Point(400, 302))); // gives contour with center of 426, 300
        Contour contour = new Contour(mat);
        var pTarget = new PotentialTarget(contour);

        var imageSize = new Size(800, 600);

        var setting =
                new TrackedTarget.TargetCalculationParameters(
                        false,
                        TargetOffsetPointEdge.Center,
                        new Point(0, 0),
                        new Point(imageSize.width / 2, imageSize.height / 2),
                        new DoubleCouple(0.0, 0.0),
                        RobotOffsetPointMode.None,
                        61,
                        34.3,
                        imageSize.area());

        var trackedTarget = new TrackedTarget(pTarget, setting);
        assertEquals(1.4, trackedTarget.getYaw(), 0.025, "Yaw was incorrect");
        assertEquals(0, trackedTarget.getPitch(), 0.025, "Pitch was incorrect");
    }
}
