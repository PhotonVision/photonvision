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

package org.photonvision.vision.processes;

import edu.wpi.cscore.VideoMode;
import java.util.ArrayList;
import java.util.HashMap;
import org.junit.jupiter.api.*;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.datatransfer.DataConsumer;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.provider.FileFrameProvider;
import org.photonvision.vision.pipeline.CVPipelineResult;

public class VisionModuleManagerTest {

    @BeforeEach
    public void init() {
        TestUtils.loadLibraries();
    }

    private static class TestSource implements VisionSource {

        private final FrameProvider provider;

        public TestSource(FrameProvider provider) {

            this.provider = provider;
        }

        @Override
        public FrameProvider getFrameProvider() {
            return provider;
        }

        @Override
        public VisionSourceSettables getSettables() {
            return new TestSettables(new CameraConfiguration("", "", "", ""));
        }
    }

    private static class TestSettables extends VisionSourceSettables {

        protected TestSettables(CameraConfiguration configuration) {
            super(configuration);
        }

        @Override
        public int getExposure() {
            return 0;
        }

        @Override
        public void setExposure(int exposure) {}

        @Override
        public int getBrightness() {
            return 0;
        }

        @Override
        public void setBrightness(int brightness) {}

        @Override
        public int getGain() {
            return 0;
        }

        @Override
        public void setGain(int gain) {}

        @Override
        public VideoMode getCurrentVideoMode() {
            return null;
        }

        @Override
        public void setCurrentVideoMode(VideoMode videoMode) {}

        @Override
        public HashMap<Integer, VideoMode> getAllVideoModes() {
            return null;
        }
    }

    private static class TestDataConsumer implements DataConsumer {
        private Data data;

        @Override
        public void accept(Data data) {
            this.data = data;
        }

        public Data getData() {
            return data;
        }
    }

    @Test
    public void setupManager() {
        var sources = new ArrayList<VisionSource>();
        sources.add(
                new TestSource(
                        new FileFrameProvider(
                                TestUtils.getWPIImagePath(TestUtils.WPI2019Image.kCargoStraightDark72in_HighRes),
                                TestUtils.WPI2019Image.FOV)));

        var moduleManager = new VisionModuleManager(sources);
        var module0DataConsumer = new TestDataConsumer();

        moduleManager.visionModules.get(0).addDataConsumer(module0DataConsumer);

        moduleManager.startModules();

        sleep(500);

        Assertions.assertNotNull(module0DataConsumer.data);
        Assertions.assertNotNull(module0DataConsumer.data.result);
        printTestResults(module0DataConsumer.data.result);
    }

    private static void printTestResults(CVPipelineResult pipelineResult) {
        double fps = 1000 / pipelineResult.getLatencyMillis();
        System.out.print(
                "Pipeline ran in " + pipelineResult.getLatencyMillis() + "ms (" + fps + " fps), ");
        System.out.println("Found " + pipelineResult.targets.size() + " valid targets");
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // ignored
        }
    }
}
