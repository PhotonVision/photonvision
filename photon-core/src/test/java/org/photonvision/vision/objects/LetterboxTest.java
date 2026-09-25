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

package org.photonvision.vision.objects;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.*;
import org.photonvision.common.LoadJNI;
import org.photonvision.vision.pipe.impl.NeuralNetworkPipeResult;

class LetterboxTest {
    @BeforeAll
    static void loadNatives() {
        LoadJNI.loadLibraries();
    }

    @Test
    void grayscaleModelInputPreservesPixelsPaddingAndCoordinates() {
        Mat gray = new Mat(200, 400, CvType.CV_8UC1, new Scalar(63));
        Mat model = new Mat();
        try {
            var transform =
                    Letterbox.letterboxBgr(gray, model, new Size(100, 100), new Scalar(128, 128, 128));
            assertEquals(3, model.channels());
            assertEquals(new Size(100, 100), model.size());
            assertArrayEquals(new double[] {63, 63, 63}, model.get(50, 50));
            assertArrayEquals(new double[] {128, 128, 128}, model.get(0, 50));
            assertEquals(1, gray.channels());
            assertEquals(new Size(400, 200), gray.size());
            var detection =
                    new NeuralNetworkPipeResult(
                            new RotatedRect(new Point(50, 50), new Size(10, 20), 0), 0, 0.9);
            var restored = transform.resizeDetections(List.of(detection)).get(0).bbox();
            assertEquals(200, restored.center.x, 1e-9);
            assertEquals(100, restored.center.y, 1e-9);
            assertEquals(40, restored.size.width, 1e-9);
            assertEquals(80, restored.size.height, 1e-9);
        } finally {
            gray.release();
            model.release();
        }
    }

    @Test
    void colorInputIsUnchangedAndReusableAfterGrayscale() {
        Mat gray = new Mat(30, 60, CvType.CV_8UC1, new Scalar(50));
        Mat color = new Mat(30, 60, CvType.CV_8UC3, new Scalar(10, 80, 210));
        Mat actual = new Mat(), expected = new Mat();
        try {
            Letterbox.letterboxBgr(gray, actual, new Size(80, 80), new Scalar(128, 128, 128));
            Letterbox.letterboxBgr(color, actual, new Size(80, 80), new Scalar(128, 128, 128));
            Letterbox.letterbox(color, expected, new Size(80, 80), new Scalar(128, 128, 128));
            assertEquals(0, Core.norm(actual, expected, Core.NORM_INF));
        } finally {
            gray.release();
            color.release();
            actual.release();
            expected.release();
        }
    }
}
