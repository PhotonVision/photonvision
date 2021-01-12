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

import edu.wpi.cscore.VideoMode;
import edu.wpi.first.wpilibj.geometry.Rotation2d;
import java.util.HashMap;
import org.junit.jupiter.api.*;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.dataflow.CVPipelineResultConsumer;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.frame.provider.FileFrameProvider;
import org.photonvision.vision.pipeline.result.CVPipelineResult;

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

        @Override
        public boolean isVendorCamera() {
            return false;
        }
    }

    private static class TestSettables extends VisionSourceSettables {

        protected TestSettables(CameraConfiguration configuration) {
            super(configuration);
        }

        @Override
        public void setExposure(double exposure) {}

        @Override
        public void setBrightness(int brightness) {}

        @Override
        public void setGain(int gain) {}

        @Override
        public VideoMode getCurrentVideoMode() {
            return new VideoMode(0, 320, 240, 254);
        }

        @Override
        public void setVideoModeInternal(VideoMode videoMode) {
            this.frameStaticProperties =
                    new FrameStaticProperties(getCurrentVideoMode(), getFOV(), new Rotation2d(), null);
        }

        @Override
        public HashMap<Integer, VideoMode> getAllVideoModes() {
            var ret = new HashMap<Integer, VideoMode>();
            ret.put(0, getCurrentVideoMode());
            return ret;
        }
    }

    private static class TestDataConsumer implements CVPipelineResultConsumer {
        private CVPipelineResult result;

        @Override
        public void accept(CVPipelineResult result) {
            this.result = result;
        }
    }

    @Test
    public void setupManager() {
        ConfigManager.getInstance().load();
        var sources = new HashMap<VisionSource, CameraConfiguration>();
        sources.put(
                new TestSource(
                        new FileFrameProvider(
                                TestUtils.getWPIImagePath(
                                        TestUtils.WPI2019Image.kCargoStraightDark72in_HighRes, false),
                                TestUtils.WPI2019Image.FOV)),
                new CameraConfiguration("Foo", "Barr"));

        var modules = VisionModuleManager.getInstance().addSources(sources);
        var module0DataConsumer = new TestDataConsumer();

        VisionModuleManager.getInstance().visionModules.get(0).addResultConsumer(module0DataConsumer);

        modules.forEach(VisionModule::start);

        sleep(1500);

        Assertions.assertNotNull(module0DataConsumer.result);
        printTestResults(module0DataConsumer.result);
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
