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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.photonvision.common.LoadJNI;
import org.photonvision.vision.opencv.CVMat;

public class StaticCropPipeTest {
    @BeforeAll
    public static void init() {
        LoadJNI.loadLibraries();
    }

    /**
     * Build a single-channel 8-bit Mat where every pixel encodes its position as row * 10 + col.
     * Dimensions must be small enough that row * 10 + col stays within a byte (0-255).
     */
    private static CVMat positionEncodedMat(int rows, int cols) {
        Mat mat = new Mat(rows, cols, CvType.CV_8UC1);
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                mat.put(row, col, new byte[] {(byte) (row * 10 + col)});
            }
        }
        return new CVMat(mat);
    }

    @Test
    public void cropReducesImageToRectDimensions() {
        StaticCropPipe pipe = new StaticCropPipe();
        pipe.setParams(new Rect(10, 20, 40, 30));

        CVMat in = positionEncodedMat(100, 100);
        CVMat out = pipe.run(in).output;

        assertNotNull(out, "Cropping a non-empty Mat with a valid Rect should produce output");
        assertEquals(40, out.getMat().cols(), "Cropped width should match the Rect width");
        assertEquals(30, out.getMat().rows(), "Cropped height should match the Rect height");

        out.release();
        in.release();
    }

    @Test
    public void cropSelectsTheCorrectRegion() {
        StaticCropPipe pipe = new StaticCropPipe();
        // x=2, y=1, width=3, height=2 -> rows [1, 3), cols [2, 5)
        pipe.setParams(new Rect(2, 1, 3, 2));

        CVMat in = positionEncodedMat(8, 8);
        CVMat out = pipe.run(in).output;

        assertNotNull(out);
        Mat cropped = out.getMat();
        assertEquals(3, cropped.cols());
        assertEquals(2, cropped.rows());

        byte[] pixel = new byte[1];
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                cropped.get(row, col, pixel);
                int expected = (row + 1) * 10 + (col + 2);
                assertEquals(
                        expected,
                        pixel[0] & 0xFF,
                        "Cropped pixel (" + row + "," + col + ") should map to the source region");
            }
        }

        out.release();
        in.release();
    }

    @Test
    public void nullCropAreaReturnsNull() {
        StaticCropPipe pipe = new StaticCropPipe();

        CVMat in = positionEncodedMat(10, 10);
        CVMat out = pipe.run(in).output;

        assertNull(out, "With no crop area configured the pipe should return null");

        in.release();
    }

    @Test
    public void emptyMatReturnsNull() {
        StaticCropPipe pipe = new StaticCropPipe();
        pipe.setParams(new Rect(0, 0, 5, 5));

        CVMat in = new CVMat(new Mat());
        CVMat out = pipe.run(in).output;

        assertNull(out, "An empty input Mat should return null");

        in.release();
    }

    @Test
    public void setParamsStoresCropArea() {
        StaticCropPipe pipe = new StaticCropPipe();
        Rect rect = new Rect(5, 6, 7, 8);
        pipe.setParams(rect);

        assertEquals(rect, pipe.getParams(), "getParams should return the configured crop area");
    }

    @Test
    public void fullFrameCropIsIdentity() {
        StaticCropPipe pipe = new StaticCropPipe();
        pipe.setParams(new Rect(0, 0, 4, 4));

        Mat source = new Mat(4, 4, CvType.CV_8UC1, new Scalar(42));
        CVMat in = new CVMat(source);
        CVMat out = pipe.run(in).output;

        assertNotNull(out);
        assertEquals(4, out.getMat().cols());
        assertEquals(4, out.getMat().rows());

        byte[] expected = new byte[16];
        byte[] actual = new byte[16];
        source.get(0, 0, expected);
        out.getMat().get(0, 0, actual);
        assertArrayEquals(expected, actual, "A full-frame crop should preserve every pixel");

        out.release();
        in.release();
    }
}
