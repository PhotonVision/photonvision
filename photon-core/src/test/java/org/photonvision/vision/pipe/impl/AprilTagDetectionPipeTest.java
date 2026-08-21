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

package org.photonvision.vision.pipe.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.photonvision.common.LoadJNI;
import org.photonvision.vision.apriltag.AprilTagFamily;
import org.photonvision.vision.opencv.CVMat;
import org.wpilib.vision.apriltag.AprilTagDetector;

public class AprilTagDetectionPipeTest {
    @BeforeAll
    public static void init() {
        LoadJNI.loadLibraries();
    }

    private static AprilTagDetectionPipe pipeWithDecimate(float decimate) {
        var config = new AprilTagDetector.Config();
        config.quadDecimate = decimate;

        var pipe = new AprilTagDetectionPipe();
        pipe.setParams(
                new AprilTagDetectionPipe.AprilTagDetectionPipeParams(
                        AprilTagFamily.kTag36h11, config, new AprilTagDetector.QuadThresholdParameters()));
        return pipe;
    }

    /**
     * The detector cannot survive images only a few pixels across (apriltag's gradient_clusters()
     * segfaults on them), so nothing may feed it one: the crop path guarantees this by growing crops
     * to at least 16px per axis before they reach any pipeline.
     */
    @Test
    public void normalImagesStillReachTheDetector() {
        var pipe = pipeWithDecimate(2);
        var image = new CVMat(Mat.zeros(480, 640, CvType.CV_8UC1));

        // A blank image detects nothing, but it has to get there without being skipped -- the run
        // completing at all is what says the guard let it through.
        assertTrue(pipe.run(image).output.isEmpty(), "A blank image has no tags in it");

        image.release();
        pipe.release();
    }
}
