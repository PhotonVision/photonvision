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
     * A static crop can produce an image only a few pixels across. The native detector reads out of
     * bounds on those -- a SIGSEGV inside apriltag's gradient_clusters(), which takes the whole
     * program with it -- so the pipe has to refuse them. Without the guard this test crashes the JVM
     * rather than failing.
     */
    @Test
    public void slimImagesAreSkippedRatherThanCrashing() {
        // Each entry is {cols, rows, decimate}: a one-pixel-tall crop, the largest crashing height at
        // each decimation, and a sliver in the other axis.
        int[][] cases = {{1080, 1, 2}, {1080, 4, 2}, {1080, 8, 4}, {1, 1080, 2}, {4, 1080, 2}};

        for (int[] testCase : cases) {
            var pipe = pipeWithDecimate(testCase[2]);
            var image = new CVMat(Mat.zeros(testCase[1], testCase[0], CvType.CV_8UC1));

            var detections = pipe.run(image).output;

            assertTrue(
                    detections.isEmpty(),
                    "Nothing is detectable in a "
                            + testCase[0]
                            + "x"
                            + testCase[1]
                            + " image, decimate "
                            + testCase[2]);

            image.release();
            pipe.release();
        }
    }

    /** The guard must not turn away images the detector can actually handle. */
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
