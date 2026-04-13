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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.photonvision.common.LoadJNI.JNITypes;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.util.TestUtils;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.common.util.numbers.NumberListUtils;
import org.photonvision.vision.apriltag.AprilTagFamily;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.frame.provider.FileFrameProvider;
import org.photonvision.vision.pipeline.AprilTagPipeline;

public class AprilTagBenchmarkTest {
    @BeforeAll
    public static void init() {
        LoadJNI.loadLibraries();
        ConfigManager.getInstance().load();
        try {
            LoadJNI.forceLoad(JNITypes.NVIDIA_APRILTAG);
        } catch (IOException ignored) {
            // GPU benchmark will transparently fall back to CPU if the optional JNI is unavailable.
        }
    }

    @AfterEach
    public void cleanup() {
        System.clearProperty("photonvision.apriltag.backend");
    }

    @Test
    public void benchmarkTag1CpuVsAuto() {
        benchmarkImage("CPU", "cpu", TestUtils.ApriltagTestImages.kTag1_640_480, 3);
        benchmarkImage("AUTO", "auto", TestUtils.ApriltagTestImages.kTag1_640_480, 3);
    }

    @Test
    public void benchmarkStressCpuVsAuto() {
        benchmarkImage("CPU", "cpu", TestUtils.ApriltagTestImages.k36h11_stress_test, 3);
        benchmarkImage("AUTO", "auto", TestUtils.ApriltagTestImages.k36h11_stress_test, 3);
    }

    private static void benchmarkImage(
            String label, String backend, TestUtils.ApriltagTestImages image, int secondsToRun) {
        System.setProperty("photonvision.apriltag.backend", backend);

        var pipeline = new AprilTagPipeline();
        pipeline.getSettings().tagFamily = AprilTagFamily.kTag36h11;
        pipeline.getSettings().solvePNPEnabled = false;
        pipeline.getSettings().outputShouldDraw = false;
        if (image == TestUtils.ApriltagTestImages.k36h11_stress_test) {
            pipeline.getSettings().decimate = 4;
        }

        var frameProvider =
                new FileFrameProvider(
                        TestUtils.getApriltagImagePath(image, false),
                        TestUtils.WPI2020Image.FOV,
                        TestUtils.get2020LifeCamCoeffs(false));
        frameProvider.requestFrameThresholdType(pipeline.getThresholdType());

        for (int i = 0; i < 5; i++) {
            pipeline.run(frameProvider.get(), QuirkyCamera.DefaultCamera).release();
        }

        var processingTimes = new ArrayList<Double>();
        var benchmarkStartMillis = System.currentTimeMillis();
        do {
            var result = pipeline.run(frameProvider.get(), QuirkyCamera.DefaultCamera);
            processingTimes.add(result.getProcessingMillis());
            result.release();
        } while (System.currentTimeMillis() - benchmarkStartMillis < secondsToRun * 1000.0);

        var processingMin = Collections.min(processingTimes);
        var processingMean = NumberListUtils.mean(processingTimes);
        var processingMax = Collections.max(processingTimes);

        System.out.println(
                label
                        + " "
                        + image.name()
                        + " processing - Min: "
                        + MathUtils.roundTo(processingMin, 3)
                        + "ms ("
                        + MathUtils.roundTo(1000 / processingMin, 3)
                        + " FPS), Mean: "
                        + MathUtils.roundTo(processingMean, 3)
                        + "ms ("
                        + MathUtils.roundTo(1000 / processingMean, 3)
                        + " FPS), Max: "
                        + MathUtils.roundTo(processingMax, 3)
                        + "ms ("
                        + MathUtils.roundTo(1000 / processingMax, 3)
                        + " FPS)");

        pipeline.release();
        frameProvider.release();
    }
}
