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

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.*;
import org.photonvision.common.LoadJNI;
import org.photonvision.vision.frame.*;
import org.photonvision.vision.opencv.CVMat;

class GrayscaleOutputStreamTest {
    @BeforeAll
    static void loadNatives() {
        LoadJNI.loadLibraries();
    }

    @Test
    void grayscaleStreamsAndCropContextBecomeBgrOnlyAtPreviewSize() {
        var settings = new AprilTagPipelineSettings();
        settings.streamingFrameDivisor = FrameDivisor.SIXTH;
        settings.outputShouldDraw = false;
        var pipeline = new OutputStreamPipeline();
        try (var frame =
                new Frame(
                        1,
                        new CVMat(new Mat(120, 180, CvType.CV_8UC1, new Scalar(50))),
                        new CVMat(new Mat(120, 180, CvType.CV_8UC1, new Scalar(60))),
                        FrameThresholdType.GREYSCALE,
                        123456,
                        new FrameStaticProperties(180, 120, 70, null))) {
            frame.contextColorImage = new CVMat(new Mat(120, 180, CvType.CV_8UC1, new Scalar(70)));
            pipeline.process(frame, settings, List.of());
            for (var image : List.of(frame.colorImage, frame.processedImage, frame.contextColorImage)) {
                assertEquals(3, image.getMat().channels());
                assertEquals(new Size(30, 20), image.getMat().size());
            }
            assertEquals(123456, frame.timestampNanos);
        } finally {
            pipeline.release();
        }
    }
}
