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

package org.photonvision.common;

/*
 * Copyright (C) 2020 Photon Vision.
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
import static org.photonvision.common.BenchmarkTest.benchmarkPipeline;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.frame.provider.FileFrameProvider;
import org.photonvision.vision.opencv.ContourGroupingMode;
import org.photonvision.vision.opencv.ContourIntersectionDirection;
import org.photonvision.vision.opencv.ContourShape;
import org.photonvision.vision.pipeline.ColoredShapePipeline;

/** Various tests that check performance on long-running tasks (i.e. a pipeline) */
public class ShapeBenchmarkTest {
    @BeforeAll
    public static void init() {
        LoadJNI.loadLibraries();
    }

    @Test
    public void Shape240pBenchmark() {
        var pipeline = new ColoredShapePipeline();
        pipeline.getSettings().hsvHue.set(60, 100);
        pipeline.getSettings().hsvSaturation.set(100, 255);
        pipeline.getSettings().hsvValue.set(190, 255);
        pipeline.getSettings().outputShouldDraw = true;
        pipeline.getSettings().outputMaximumTargets = 20;
        pipeline.getSettings().contourGroupingMode = ContourGroupingMode.Single;
        pipeline.getSettings().contourIntersection = ContourIntersectionDirection.Up;
        pipeline.getSettings().contourShape = ContourShape.Custom;
        pipeline.getSettings().circleDetectThreshold = 10;
        pipeline.getSettings().accuracyPercentage = 30.0;
        try (var frameProvider =
                new FileFrameProvider(
                        TestUtils.getWPIImagePath(TestUtils.WPI2019Image.kCargoSideStraightDark72in, false),
                        TestUtils.WPI2019Image.FOV)) {
            benchmarkPipeline(frameProvider, pipeline, 5);
        }
    }

    @Test
    public void Shape480pBenchmark() {
        var pipeline = new ColoredShapePipeline();
        pipeline.getSettings().hsvHue.set(60, 100);
        pipeline.getSettings().hsvSaturation.set(100, 255);
        pipeline.getSettings().hsvValue.set(190, 255);
        pipeline.getSettings().outputShouldDraw = true;
        pipeline.getSettings().outputMaximumTargets = 20;
        pipeline.getSettings().contourGroupingMode = ContourGroupingMode.Single;
        pipeline.getSettings().contourIntersection = ContourIntersectionDirection.Up;
        pipeline.getSettings().contourShape = ContourShape.Custom;
        pipeline.getSettings().circleDetectThreshold = 10;
        pipeline.getSettings().accuracyPercentage = 30.0;

        try (var frameProvider =
                new FileFrameProvider(
                        TestUtils.getWPIImagePath(TestUtils.WPI2020Image.kBlueGoal_084in_Center, false),
                        TestUtils.WPI2020Image.FOV)) {
            benchmarkPipeline(frameProvider, pipeline, 5);
        }
    }

    @Test
    public void Shape720pBenchmark() {
        var pipeline = new ColoredShapePipeline();
        pipeline.getSettings().hsvHue.set(60, 100);
        pipeline.getSettings().hsvSaturation.set(100, 255);
        pipeline.getSettings().hsvValue.set(190, 255);
        pipeline.getSettings().outputShouldDraw = true;
        pipeline.getSettings().outputMaximumTargets = 20;
        pipeline.getSettings().contourGroupingMode = ContourGroupingMode.Single;
        pipeline.getSettings().contourIntersection = ContourIntersectionDirection.Up;
        pipeline.getSettings().contourShape = ContourShape.Custom;
        pipeline.getSettings().circleDetectThreshold = 10;
        pipeline.getSettings().accuracyPercentage = 30.0;

        try (var frameProvider =
                new FileFrameProvider(
                        TestUtils.getWPIImagePath(TestUtils.WPI2020Image.kBlueGoal_084in_Center_720p, false),
                        TestUtils.WPI2020Image.FOV)) {
            benchmarkPipeline(frameProvider, pipeline, 5);
        }
    }

    @Test
    public void Shape1920x1440Benchmark() {
        var pipeline = new ColoredShapePipeline();
        pipeline.getSettings().hsvHue.set(60, 100);
        pipeline.getSettings().hsvSaturation.set(100, 255);
        pipeline.getSettings().hsvValue.set(190, 255);
        pipeline.getSettings().outputShouldDraw = true;
        pipeline.getSettings().outputMaximumTargets = 20;
        pipeline.getSettings().contourGroupingMode = ContourGroupingMode.Single;
        pipeline.getSettings().contourIntersection = ContourIntersectionDirection.Up;
        pipeline.getSettings().contourShape = ContourShape.Custom;
        pipeline.getSettings().circleDetectThreshold = 10;
        pipeline.getSettings().accuracyPercentage = 30.0;

        try (var frameProvider =
                new FileFrameProvider(
                        TestUtils.getWPIImagePath(TestUtils.WPI2019Image.kCargoStraightDark72in_HighRes, false),
                        TestUtils.WPI2019Image.FOV)) {
            benchmarkPipeline(frameProvider, pipeline, 5);
        }
    }
}
