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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.photonvision.common.util.TestUtils;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.common.util.numbers.NumberListUtils;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.provider.FileFrameProvider;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.ContourGroupingMode;
import org.photonvision.vision.opencv.ContourIntersectionDirection;
import org.photonvision.vision.pipeline.CVPipeline;
import org.photonvision.vision.pipeline.ReflectivePipeline;
import org.photonvision.vision.pipeline.result.CVPipelineResult;

/** Various tests that check performance on long-running tasks (i.e. a pipeline) */
public class BenchmarkTest {
    @BeforeAll
    public static void init() {
        TestUtils.loadLibraries();
    }

    @Test
    @Order(1)
    public void Reflective240pBenchmark() {
        var pipeline = new ReflectivePipeline();
        pipeline.getSettings().hsvHue.set(60, 100);
        pipeline.getSettings().hsvSaturation.set(100, 255);
        pipeline.getSettings().hsvValue.set(190, 255);
        pipeline.getSettings().outputShouldDraw = true;
        pipeline.getSettings().outputShowMultipleTargets = true;
        pipeline.getSettings().contourGroupingMode = ContourGroupingMode.Dual;
        pipeline.getSettings().contourIntersection = ContourIntersectionDirection.Up;

        var frameProvider =
                new FileFrameProvider(
                        TestUtils.getWPIImagePath(TestUtils.WPI2019Image.kCargoSideStraightDark72in, false),
                        TestUtils.WPI2019Image.FOV);

        benchmarkPipeline(frameProvider, pipeline, 5);
    }

    @Test
    @Order(1)
    public void Reflective480pBenchmark() {
        var pipeline = new ReflectivePipeline();
        pipeline.getSettings().hsvHue.set(60, 100);
        pipeline.getSettings().hsvSaturation.set(200, 255);
        pipeline.getSettings().hsvValue.set(200, 255);
        pipeline.getSettings().outputShouldDraw = true;

        var frameProvider =
                new FileFrameProvider(
                        TestUtils.getWPIImagePath(TestUtils.WPI2020Image.kBlueGoal_084in_Center, false),
                        TestUtils.WPI2020Image.FOV);

        benchmarkPipeline(frameProvider, pipeline, 5);
    }

    @Test
    @Order(3)
    public void Reflective720pBenchmark() {
        var pipeline = new ReflectivePipeline();
        pipeline.getSettings().hsvHue.set(60, 100);
        pipeline.getSettings().hsvSaturation.set(200, 255);
        pipeline.getSettings().hsvValue.set(200, 255);
        pipeline.getSettings().outputShouldDraw = true;

        var frameProvider =
                new FileFrameProvider(
                        TestUtils.getWPIImagePath(TestUtils.WPI2020Image.kBlueGoal_084in_Center_720p, false),
                        TestUtils.WPI2020Image.FOV);

        benchmarkPipeline(frameProvider, pipeline, 5);
    }

    @Test
    @Order(4)
    public void Reflective1920x1440Benchmark() {
        var pipeline = new ReflectivePipeline();
        pipeline.getSettings().hsvHue.set(60, 100);
        pipeline.getSettings().hsvSaturation.set(100, 255);
        pipeline.getSettings().hsvValue.set(190, 255);
        pipeline.getSettings().outputShouldDraw = true;
        pipeline.getSettings().outputShowMultipleTargets = true;
        pipeline.getSettings().contourGroupingMode = ContourGroupingMode.Dual;
        pipeline.getSettings().contourIntersection = ContourIntersectionDirection.Up;

        var frameProvider =
                new FileFrameProvider(
                        TestUtils.getWPIImagePath(TestUtils.WPI2019Image.kCargoStraightDark72in_HighRes, false),
                        TestUtils.WPI2019Image.FOV);

        benchmarkPipeline(frameProvider, pipeline, 5);
    }

    private static <P extends CVPipeline> void benchmarkPipeline(
            FrameProvider frameProvider, P pipeline, int secondsToRun) {
        CVMat.enablePrint(false);
        // warmup for 5 loops.
        System.out.println("Warming up for 5 loops...");
        for (int i = 0; i < 5; i++) {
            pipeline.run(frameProvider.get(), QuirkyCamera.DefaultCamera);
        }

        final List<Double> processingTimes = new ArrayList<>();
        final List<Double> latencyTimes = new ArrayList<>();

        var frameProps = frameProvider.get().frameStaticProperties;

        // begin benchmark
        System.out.println(
                "Beginning "
                        + secondsToRun
                        + " second benchmark at resolution "
                        + frameProps.imageWidth
                        + "x"
                        + frameProps.imageHeight);
        var benchmarkStartMillis = System.currentTimeMillis();
        do {
            CVPipelineResult pipelineResult =
                    pipeline.run(frameProvider.get(), QuirkyCamera.DefaultCamera);
            pipelineResult.release();
            processingTimes.add(pipelineResult.getProcessingMillis());
            latencyTimes.add(pipelineResult.getLatencyMillis());
        } while (System.currentTimeMillis() - benchmarkStartMillis < secondsToRun * 1000.0);
        System.out.println("Benchmark complete.");

        var processingMin = Collections.min(processingTimes);
        var processingMean = NumberListUtils.mean(processingTimes);
        var processingMax = Collections.max(processingTimes);

        var latencyMin = Collections.min(latencyTimes);
        var latencyMean = NumberListUtils.mean(latencyTimes);
        var latencyMax = Collections.max(latencyTimes);

        String processingResult =
                "Processing times - "
                        + "Min: "
                        + MathUtils.roundTo(processingMin, 3)
                        + "ms ("
                        + MathUtils.roundTo(1000 / processingMin, 3)
                        + " FPS), "
                        + "Mean: "
                        + MathUtils.roundTo(processingMean, 3)
                        + "ms ("
                        + MathUtils.roundTo(1000 / processingMean, 3)
                        + " FPS), "
                        + "Max: "
                        + MathUtils.roundTo(processingMax, 3)
                        + "ms ("
                        + MathUtils.roundTo(1000 / processingMax, 3)
                        + " FPS)";
        System.out.println(processingResult);
        String latencyResult =
                "Latency times - "
                        + "Min: "
                        + MathUtils.roundTo(latencyMin, 3)
                        + "ms ("
                        + MathUtils.roundTo(1000 / latencyMin, 3)
                        + " FPS), "
                        + "Mean: "
                        + MathUtils.roundTo(latencyMean, 3)
                        + "ms ("
                        + MathUtils.roundTo(1000 / latencyMean, 3)
                        + " FPS), "
                        + "Max: "
                        + MathUtils.roundTo(latencyMax, 3)
                        + "ms ("
                        + MathUtils.roundTo(1000 / latencyMax, 3)
                        + " FPS)";
        System.out.println(latencyResult);
    }
}
