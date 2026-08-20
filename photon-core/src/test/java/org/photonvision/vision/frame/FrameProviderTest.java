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

package org.photonvision.vision.frame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.Rect;
import org.photonvision.common.LoadJNI;

public class FrameProviderTest {
    @BeforeAll
    public static void init() {
        LoadJNI.loadLibraries();
    }

    @Test
    public void cropIsClampedIntoTheImage() {
        // Overhangs the right and bottom edges, so the crop has to stop at them.
        var clamped = FrameProvider.clampCropToImage(new Rect(600, 400, 5000, 5000), 640, 480);

        assertEquals(600, clamped.x);
        assertEquals(400, clamped.y);
        assertEquals(40, clamped.width, "Width should stop at the right edge");
        assertEquals(80, clamped.height, "Height should stop at the bottom edge");

        // Clamping an oversized crop can leave it covering everything, which is a no-op.
        assertNull(
                FrameProvider.clampCropToImage(new Rect(-50, -50, 5000, 5000), 640, 480),
                "A crop swallowing the whole image should come back as no crop");
    }

    @Test
    public void emptyAndWholeImageCropsAreNoOps() {
        assertNull(FrameProvider.clampCropToImage(null, 640, 480), "A null crop is a no-op");
        assertNull(
                FrameProvider.clampCropToImage(new Rect(0, 0, 0, 0), 640, 480), "An empty crop is a no-op");
        assertNull(
                FrameProvider.clampCropToImage(new Rect(0, 0, 640, 480), 640, 480),
                "A crop covering the whole image is a no-op");
    }

    @Test
    public void sliverCropsAreGrownToAUsableSize() {
        // A one-pixel-tall crop crashes the native apriltag detector, so it must never reach a
        // pipeline.
        var thin = FrameProvider.clampCropToImage(new Rect(0, 100, 640, 1), 640, 480);
        assertTrue(thin.height >= 16, "A one-pixel-tall crop should be grown, got " + thin);
        assertEquals(640, thin.width, "The wide axis should be left alone");

        var narrow = FrameProvider.clampCropToImage(new Rect(100, 0, 1, 480), 640, 480);
        assertTrue(narrow.width >= 16, "A one-pixel-wide crop should be grown, got " + narrow);
    }

    @Test
    public void grownCropsStayInsideTheImage() {
        // A sliver against the far edge has to grow inwards, not off the end of the image.
        var corner = FrameProvider.clampCropToImage(new Rect(639, 479, 1, 1), 640, 480);

        assertTrue(corner.x >= 0 && corner.y >= 0, "Crop origin should stay in the image: " + corner);
        assertTrue(corner.x + corner.width <= 640, "Crop should end inside the image: " + corner);
        assertTrue(corner.y + corner.height <= 480, "Crop should end inside the image: " + corner);
        assertTrue(corner.width >= 16 && corner.height >= 16, "Crop should be usable: " + corner);
    }

    @Test
    public void cropsInATinyImageCannotExceedIt() {
        // The image is smaller than the minimum crop, so the crop caps out at the image -- which makes
        // it a whole-image no-op rather than a rectangle reaching past the buffer.
        assertNull(
                FrameProvider.clampCropToImage(new Rect(0, 0, 2, 2), 8, 8),
                "A crop in an image smaller than the minimum should degrade to no crop");
    }
}
