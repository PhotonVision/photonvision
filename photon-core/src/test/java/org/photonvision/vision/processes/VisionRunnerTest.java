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

package org.photonvision.vision.processes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.Rect;
import org.photonvision.common.LoadJNI;
import org.photonvision.common.util.numbers.IntegerCouple;
import org.photonvision.vision.pipeline.AdvancedPipelineSettings;
import org.photonvision.vision.pipeline.AprilTagPipelineSettings;
import org.photonvision.vision.pipeline.ReflectivePipelineSettings;

public class VisionRunnerTest {
    @BeforeAll
    public static void init() {
        LoadJNI.loadLibraries();
    }

    private static AdvancedPipelineSettings settings(IntegerCouple x, IntegerCouple y) {
        var settings = new ReflectivePipelineSettings();
        settings.staticCropEnabled = true;
        settings.staticCropX = x;
        settings.staticCropY = y;
        return settings;
    }

    @Test
    public void disabledCropProducesNoRectangle() {
        var settings = settings(new IntegerCouple(10, 100), new IntegerCouple(10, 100));
        settings.staticCropEnabled = false;

        assertNull(VisionRunner.cropRectFromSettings(settings));
    }

    @Test
    public void apriltagCropOriginIsAlignedToTheTileGrid() {
        // apriltag thresholds the decimated image in 4x4 tiles, so an origin off a multiple of
        // 4 * decimate moves the tiling relative to the tag and shifts the pose it reports.
        var settings = new AprilTagPipelineSettings();
        settings.staticCropEnabled = true;
        settings.staticCropX = new IntegerCouple(201, 501);
        settings.staticCropY = new IntegerCouple(151, 401);

        settings.decimate = 1;
        var rect = VisionRunner.cropRectFromSettings(settings);
        assertEquals(200, rect.x, "x should drop to a multiple of 4");
        assertEquals(148, rect.y, "y should drop to a multiple of 4");
        assertEquals(501, rect.x + rect.width, "The requested region should still be covered");
        assertEquals(401, rect.y + rect.height, "The requested region should still be covered");

        settings.decimate = 4;
        rect = VisionRunner.cropRectFromSettings(settings);
        assertEquals(192, rect.x, "x should drop to a multiple of 16 at decimate 4");
        assertEquals(144, rect.y, "y should drop to a multiple of 16 at decimate 4");
        assertEquals(501, rect.x + rect.width, "The requested region should still be covered");
        assertEquals(401, rect.y + rect.height, "The requested region should still be covered");
    }

    /**
     * Only the apriltag detector cares about the tiling, so other pipelines get what they asked for.
     */
    @Test
    public void rangesBecomeARectangle() {
        var rect =
                VisionRunner.cropRectFromSettings(
                        settings(new IntegerCouple(10, 110), new IntegerCouple(20, 70)));

        assertEquals(new Rect(10, 20, 100, 50), rect);
    }

    @Test
    public void reversedRangesAreNormalized() {
        var rect =
                VisionRunner.cropRectFromSettings(
                        settings(new IntegerCouple(110, 10), new IntegerCouple(70, 20)));

        assertEquals(new Rect(10, 20, 100, 50), rect);
    }

    @Test
    public void emptyRangesProduceNoRectangle() {
        assertNull(
                VisionRunner.cropRectFromSettings(
                        settings(new IntegerCouple(50, 50), new IntegerCouple(0, 100))),
                "A zero-width range means no crop");
        assertNull(
                VisionRunner.cropRectFromSettings(
                        settings(new IntegerCouple(0, 100), new IntegerCouple(50, 50))),
                "A zero-height range means no crop");
    }

    @Test
    public void negativeBoundsDoNotBecomeASliver() {
        // A bound that overflowed on its way in from the UI arrives as -1. Treating that as a real
        // coordinate produced a one-pixel crop, which segfaults the native apriltag detector, so the
        // sign has to be dropped instead.
        var x =
                VisionRunner.cropRectFromSettings(
                        settings(new IntegerCouple(0, -1), new IntegerCouple(0, 480)));
        assertNull(x, "A negative upper bound should not produce a one-pixel-wide crop");

        var y =
                VisionRunner.cropRectFromSettings(
                        settings(new IntegerCouple(0, 640), new IntegerCouple(0, -1)));
        assertNull(y, "A negative upper bound should not produce a one-pixel-tall crop");
    }

    @Test
    public void theFrameEdgeSentinelCoversTheWholeFrame() {
        // The UI's "to the frame edge" sentinel is Integer.MAX_VALUE; it has to survive as a huge
        // width rather than wrapping around into something degenerate.
        var rect =
                VisionRunner.cropRectFromSettings(
                        settings(
                                new IntegerCouple(0, Integer.MAX_VALUE), new IntegerCouple(0, Integer.MAX_VALUE)));

        assertEquals(0, rect.x);
        assertEquals(0, rect.y);
        assertEquals(Integer.MAX_VALUE, rect.width);
        assertEquals(Integer.MAX_VALUE, rect.height);
    }
}
