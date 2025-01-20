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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import edu.wpi.first.cscore.VideoMode;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.dataflow.CVPipelineResultConsumer;
import org.photonvision.common.util.TestUtils;
import org.photonvision.jni.PhotonTargetingJniLoader;
import org.photonvision.vision.camera.PVCameraInfo;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.camera.USBCameras.USBCameraSource;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.frame.provider.FileFrameProvider;
import org.photonvision.vision.pipeline.result.CVPipelineResult;

public class VisionModuleManagerTest {
    @BeforeAll
    public static void init() {
        String classpathStr = System.getProperty("java.class.path");
        System.out.print(classpathStr);

        TestUtils.loadLibraries();
        try {
            if (!PhotonTargetingJniLoader.load()) fail();
        } catch (UnsatisfiedLinkError | IOException e) {
            e.printStackTrace();
            fail(e);
        }
    }

    private static class TestSource extends VisionSource {
        private final FrameProvider provider;

        public TestSource(FrameProvider provider, CameraConfiguration cameraConfiguration) {
            super(cameraConfiguration);
            this.provider = provider;
            if (getCameraConfiguration().cameraQuirks == null)
                getCameraConfiguration().cameraQuirks = QuirkyCamera.DefaultCamera;
        }

        @Override
        public FrameProvider getFrameProvider() {
            return provider;
        }

        @Override
        public VisionSourceSettables getSettables() {
            return new TestSettables(getCameraConfiguration());
        }

        @Override
        public boolean isVendorCamera() {
            return false;
        }

        @Override
        public boolean hasLEDs() {
            return false;
        }

        @Override
        public void remakeSettables() {
            return;
        }

        @Override
        public void release() {}
    }

    private static class TestSettables extends VisionSourceSettables {
        protected TestSettables(CameraConfiguration configuration) {
            super(configuration);
        }

        @Override
        public void setExposureRaw(double exposure) {}

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
            this.frameStaticProperties = new FrameStaticProperties(getCurrentVideoMode(), getFOV(), null);
        }

        @Override
        public HashMap<Integer, VideoMode> getAllVideoModes() {
            var ret = new HashMap<Integer, VideoMode>();
            ret.put(0, getCurrentVideoMode());
            return ret;
        }

        @Override
        public void setAutoExposure(boolean cameraAutoExposure) {}

        @Override
        public double getMinExposureRaw() {
            return 1;
        }

        @Override
        public double getMaxExposureRaw() {
            return 1234;
        }

        @Override
        public void setAutoWhiteBalance(boolean autowb) {}

        @Override
        public void setWhiteBalanceTemp(double temp) {}

        @Override
        public double getMaxWhiteBalanceTemp() {
            return 1;
        }

        @Override
        public double getMinWhiteBalanceTemp() {
            return 2;
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

        var vmm = new VisionModuleManager();

        var conf = new CameraConfiguration(PVCameraInfo.fromFileInfo("Foo", "Bar"));
        var ffp =
                new FileFrameProvider(
                        TestUtils.getWPIImagePath(TestUtils.WPI2019Image.kCargoStraightDark72in_HighRes, false),
                        TestUtils.WPI2019Image.FOV);

        var testSource = new TestSource(ffp, conf);

        var module = vmm.addSource(testSource);
        var module0DataConsumer = new TestDataConsumer();

        module.addResultConsumer(module0DataConsumer);

        module.start();

        sleep(1500);

        Assertions.assertNotNull(module0DataConsumer.result);
        printTestResults(module0DataConsumer.result);
    }

    @Test
    public void testMultipleStreamIndex() {
        ConfigManager.getInstance().load();

        var vmm = new VisionModuleManager();

        var conf = new CameraConfiguration(PVCameraInfo.fromFileInfo("Foo", "Bar"));
        conf.streamIndex = 1;
        var ffp =
                new FileFrameProvider(
                        TestUtils.getWPIImagePath(TestUtils.WPI2019Image.kCargoStraightDark72in_HighRes, false),
                        TestUtils.WPI2019Image.FOV);
        var testSource = new TestSource(ffp, conf);

        var conf2 = new CameraConfiguration(PVCameraInfo.fromFileInfo("Foo2", "Bar2"));
        conf2.streamIndex = 0;
        var ffp2 =
                new FileFrameProvider(
                        TestUtils.getWPIImagePath(TestUtils.WPI2019Image.kCargoStraightDark72in_HighRes, false),
                        TestUtils.WPI2019Image.FOV);
        var testSource2 = new TestSource(ffp2, conf2);

        var conf3 = new CameraConfiguration(PVCameraInfo.fromFileInfo("Foo3", "Bar3"));
        conf3.streamIndex = 0;
        var ffp3 =
                new FileFrameProvider(
                        TestUtils.getWPIImagePath(TestUtils.WPI2019Image.kCargoStraightDark72in_HighRes, false),
                        TestUtils.WPI2019Image.FOV);
        var testSource3 = new TestSource(ffp3, conf3);

        // Arducam OV9281 UC844 raspberry pi test.
        var conf4 = new CameraConfiguration(PVCameraInfo.fromFileInfo("Left", "/dev/video1"));
        USBCameraSource usbSimulation = new MockUsbCameraSource(conf4, 0x6366, 0x0c45);

        var conf5 = new CameraConfiguration(PVCameraInfo.fromFileInfo("Right", "/dev/video2"));
        USBCameraSource usbSimulation2 = new MockUsbCameraSource(conf5, 0x6366, 0x0c45);

        var modules =
                List.of(testSource, testSource2, testSource3, usbSimulation, usbSimulation2).stream()
                        .map(vmm::addSource)
                        .collect(Collectors.toList());

        System.out.println(
                Arrays.toString(
                        modules.stream().map(it -> it.getCameraConfiguration().streamIndex).toArray()));
        var idxs =
                modules.stream()
                        .map(it -> it.getCameraConfiguration().streamIndex)
                        .collect(Collectors.toList());

        assertTrue(usbSimulation.equals(usbSimulation));
        assertTrue(!usbSimulation.equals(usbSimulation2));

        assertTrue(idxs.contains(0));
        assertTrue(idxs.contains(1));
        assertTrue(idxs.contains(2));
        assertTrue(idxs.contains(3));
        assertTrue(idxs.contains(4));
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
