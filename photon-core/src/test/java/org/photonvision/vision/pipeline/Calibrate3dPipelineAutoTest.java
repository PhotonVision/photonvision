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

package org.photonvision.vision.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.photonvision.common.LoadJNI;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameDivisor;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipeline.UICalibrationData.BoardType;
import org.photonvision.vision.pipeline.UICalibrationData.TagFamily;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.wpilib.math.util.Units;

public class Calibrate3dPipelineAutoTest {
    private static final Size RESOLUTION = new Size(1280, 720);
    private static final String IMAGE_RELATIVE_PATH = "lifecam/2024-05-07_lifecam_1280/img0.png";

    @BeforeAll
    public static void init() throws IOException {
        LoadJNI.loadLibraries();
    }

    private static Calibrate3dPipeline createPipeline() {
        var pipeline = new Calibrate3dPipeline();
        pipeline.getSettings().autoCalibrate = true;
        pipeline.getSettings().boardType = BoardType.CHARUCOBOARD;
        pipeline.getSettings().tagFamily = TagFamily.Dict_4X4_1000;
        pipeline.getSettings().boardHeight = 8;
        pipeline.getSettings().boardWidth = 8;
        pipeline.getSettings().gridSize = Units.inchesToMeters(1);
        pipeline.getSettings().markerSize = Units.inchesToMeters(0.75);
        pipeline.getSettings().resolution = RESOLUTION;
        pipeline.getSettings().streamingFrameDivisor = FrameDivisor.NONE;
        return pipeline;
    }

    private static void runFrame(Calibrate3dPipeline pipeline, double translateX) {
        Mat image =
                Imgcodecs.imread(
                        TestUtils.getCharucoBoardImagesPath().resolve(IMAGE_RELATIVE_PATH).toString());
        if (translateX != 0) {
            Mat translated = new Mat();
            Mat transform = Mat.eye(2, 3, CvType.CV_64F);
            transform.put(0, 2, translateX);
            Imgproc.warpAffine(image, translated, transform, image.size());
            image.release();
            transform.release();
            image = translated;
        }
        var frame =
                new Frame(
                        0,
                        new CVMat(image),
                        new CVMat(),
                        FrameThresholdType.NONE,
                        new FrameStaticProperties((int) RESOLUTION.width, (int) RESOLUTION.height, 67, null));
        try (CVPipelineResult ignored = pipeline.run(frame, QuirkyCamera.DefaultCamera)) {
            // Result is auto-closed
        }
    }

    @Test
    public void autoSnapshotsRequireStabilityAndMovement() {
        // 5% of the 1280x720 frame diagonal -- matches Calibrate3dPipeline's MIN_MOVEMENT_FRACTION
        double minMovementPx = 0.05 * Math.hypot(RESOLUTION.width, RESOLUTION.height);

        try (Calibrate3dPipeline pipeline = createPipeline()) {
            // A single frame must not snapshot: the board has not been stable for STABILITY_WINDOW frames
            runFrame(pipeline, 0);
            runFrame(pipeline, 0);
            assertEquals(0, pipeline.foundCornersList.size());

            // Three stable frames at the first position -> first snapshot
            runFrame(pipeline, 0);
            assertEquals(1, pipeline.foundCornersList.size());

            // Holding still at the same position must not take more snapshots
            runFrame(pipeline, 0);
            runFrame(pipeline, 0);
            runFrame(pipeline, 0);
            assertEquals(1, pipeline.foundCornersList.size());

            // Move the board far enough, but keep it moving between frames -> still no snapshot
            double shift = minMovementPx + 50;
            runFrame(pipeline, shift);
            runFrame(pipeline, 0);
            runFrame(pipeline, shift);
            assertEquals(1, pipeline.foundCornersList.size());

            // Settle at the new position -> snapshot
            runFrame(pipeline, shift);
            runFrame(pipeline, shift);
            assertEquals(2, pipeline.foundCornersList.size());

            // Settle back at the original position -> snapshot
            runFrame(pipeline, 0);
            runFrame(pipeline, 0);
            runFrame(pipeline, 0);
            assertEquals(3, pipeline.foundCornersList.size());
        }
    }
}
