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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wpi.first.cscore.UsbCameraInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.util.TestUtils;
import org.photonvision.common.util.file.JacksonUtils;
import org.photonvision.jni.PhotonTargetingJniLoader;
import org.photonvision.vision.camera.PVCameraInfo;

public class VisionSourceManagerTest {
    // Test harness that overrides getConnectedCameras, but uses USB cameras for
    // everything else
    // when we start testing libcamera stuff we'll need to mock more stuff out
    private static class TestVsm extends VisionSourceManager {
        public List<PVCameraInfo> testCameras = new ArrayList<>();

        @Override
        protected List<PVCameraInfo> getConnectedCameras() {
            return testCameras;
        }

        public void teardown() {
            // release native resources
            var uniqueNames = getVisionModules().stream().map(VisionModule::uniqueName).toList();
            for (var name : uniqueNames) {
                deactivateVisionSource(name);
            }
        }
    }

    @BeforeAll
    public static void loadLibraries() {
        TestUtils.loadLibraries();
        assertDoesNotThrow(PhotonTargetingJniLoader::load);
        assertTrue(PhotonTargetingJniLoader.isWorking);

        // Broadcast all still calls into configmanager (ew) so set that up here
        ConfigManager.getInstance().load();
    }

    private TestVsm vsm = null;

    @BeforeEach
    public void createVsm() {
        ConfigManager.getInstance().clearConfig();
        vsm = new TestVsm();
    }

    @AfterEach
    public void teardownVsm() {
        vsm.teardown();
    }

    @Test
    public void testCameraInfoSerde() throws InterruptedException, IOException {
        {
            var usb =
                    PVCameraInfo.fromUsbCameraInfo(
                            new UsbCameraInfo(
                                    2,
                                    "/dev/video2",
                                    "Left Camera", // renamed arducam
                                    new String[] {
                                        "/dev/v4l/by-id/usb-Arducam_Technology_Co.__Ltd._Left_Camera_12345-video-index0",
                                        "/dev/v4l/by-path/platform-xhci-hcd.0-usb-0:2:1.0-video-index0"
                                    },
                                    7,
                                    8));

            var str = JacksonUtils.serializeToString(usb);
            System.out.println(str);
            System.out.println(JacksonUtils.deserialize(str, PVCameraInfo.class));
        }
        {
            var csi =
                    PVCameraInfo.fromCSICameraInfo(
                            "/dev/v4l/by-path/platform-1f00110000.csi-video-index0", "rp1-cfe");
            var str = JacksonUtils.serializeToString(csi);
            System.out.println(str);
            System.out.println(JacksonUtils.deserialize(str, PVCameraInfo.class));
        }
    }

    @Test
    public void testEmpty() {
        var vsm = new TestVsm();

        List<CameraConfiguration> configs = List.of();
        vsm.registerLoadedConfigs(configs);

        // And make assertions about the current matching state
        assertEquals(0, vsm.getVsmState().allConnectedCameras.size());
        assertEquals(0, vsm.getVsmState().disabledConfigs.size());
        assertEquals(0, vsm.vmm.getModules().size());
    }

    @Test
    public void testFileVisionSource() throws InterruptedException, IOException {
        var fileCamera1 =
                PVCameraInfo.fromFileInfo(
                        TestUtils.getApriltagImagePath(TestUtils.ApriltagTestImages.kTag1_640_480, false)
                                .toAbsolutePath()
                                .toString(),
                        "kTag1_640_480");

        vsm.testCameras = List.of(fileCamera1);

        List<CameraConfiguration> configs = List.of();
        vsm.registerLoadedConfigs(configs);

        vsm.assignUnmatchedCamera(fileCamera1);

        System.out.println(JacksonUtils.serializeToString(ConfigManager.getInstance().getConfig()));

        // And make assertions about the current matching state
        assertEquals(1, vsm.getVsmState().allConnectedCameras.size());
        assertEquals(0, vsm.getVsmState().disabledConfigs.size());
        assertEquals(1, vsm.vmm.getModules().size());
    }

    @Test
    public void testEnabledDisabled() throws InterruptedException {
        // GIVEN a VSM
        var vsm = new TestVsm();
        // AND one enabled camera, and one disabled camera
        var enabledCam =
                new CameraConfiguration(
                        PVCameraInfo.fromUsbCameraInfo(
                                new UsbCameraInfo(
                                        0,
                                        "/dev/video0",
                                        "Lifecam HD-3000",
                                        new String[] {"/dev/v4l/by-path/foobar1"},
                                        5940,
                                        5940)));
        enabledCam.deactivated = false;
        enabledCam.nickname = "Matt's awesome camera 1";

        var disabledCam =
                new CameraConfiguration(
                        PVCameraInfo.fromUsbCameraInfo(
                                new UsbCameraInfo(
                                        1,
                                        "/dev/video1",
                                        "Lifecam HD-3000",
                                        new String[] {"/dev/v4l/by-path/foobar2"},
                                        5940,
                                        5940)));
        enabledCam.deactivated = true;
        enabledCam.nickname = "Matt's awesome camera 2";

        vsm.testCameras = List.of(enabledCam.matchedCameraInfo, disabledCam.matchedCameraInfo);

        // WHEN cameras are loaded from disk
        vsm.registerLoadedConfigs(List.of(enabledCam, disabledCam));

        // the enabled and disabled cameras will be matched
        assertEquals(2, vsm.getVsmState().allConnectedCameras.size());
        assertEquals(1, vsm.getVsmState().disabledConfigs.size());
        assertEquals(1, vsm.vmm.getModules().size());

        Thread.sleep(2000);

        vsm.teardown();
    }

    @Test
    public void testOtherPathsOrderChange() throws InterruptedException {
        // GIVEN a VSM
        var vsm = new TestVsm();
        // AND one camera and camera config with flipped otherpaths order
        var cam =
                PVCameraInfo.fromUsbCameraInfo(
                        new UsbCameraInfo(
                                0,
                                "/dev/video0",
                                "Lifecam HD-3000",
                                new String[] {"/dev/v4l/by-path/usbv2/foobar1", "/dev/v4l/by-path/usb/foobar1"},
                                5940,
                                5940));

        var camOtherPaths =
                PVCameraInfo.fromUsbCameraInfo(
                        new UsbCameraInfo(
                                1,
                                "/dev/video1",
                                "Lifecam HD-3000",
                                new String[] {"/dev/v4l/by-path/usb/foobar1", "/dev/v4l/by-path/usbv2/foobar1"},
                                5940,
                                5940));
        CameraConfiguration camOtherPathsConf = new CameraConfiguration(camOtherPaths);
        camOtherPathsConf.nickname = "TestCamera";
        camOtherPathsConf.deactivated = false;

        vsm.registerLoadedConfigs(List.of(camOtherPathsConf));

        vsm.assignUnmatchedCamera(cam);

        assertEquals(0, vsm.getVsmState().disabledConfigs.size());
        assertEquals(1, vsm.vmm.getModules().size());
        assertEquals(cam.uniquePath(), camOtherPaths.uniquePath());

        Thread.sleep(2000);

        vsm.teardown();
    }

    @Test
    public void testDuplicate() throws InterruptedException, IOException {
        var fileCamera1 =
                PVCameraInfo.fromFileInfo(
                        TestUtils.getApriltagImagePath(TestUtils.ApriltagTestImages.kTag1_640_480, false)
                                .toAbsolutePath()
                                .toString(),
                        "kTag1_640_480");
        CameraConfiguration camConf1 = new CameraConfiguration(fileCamera1);
        camConf1.deactivated = true;

        var fileCamera2 =
                PVCameraInfo.fromFileInfo(
                        TestUtils.getApriltagImagePath(TestUtils.ApriltagTestImages.kRobots, false)
                                .toAbsolutePath()
                                .toString(),
                        "kTag1_640_480");
        CameraConfiguration camConf2 = new CameraConfiguration(fileCamera2);
        camConf2.nickname = camConf1.nickname + " (1)";
        camConf2.uniqueName += "owo";
        camConf2.deactivated = true;

        var fileCamera3 =
                PVCameraInfo.fromFileInfo(
                        TestUtils.getApriltagImagePath(TestUtils.ApriltagTestImages.kTag1_640_480, false)
                                .toAbsolutePath()
                                .toString(),
                        "kTag1_640_480");

        vsm.testCameras = List.of(fileCamera1, fileCamera2, fileCamera3);

        List<CameraConfiguration> configs = List.of(camConf1, camConf2);
        vsm.registerLoadedConfigs(configs);

        vsm.assignUnmatchedCamera(fileCamera3);

        System.out.println(JacksonUtils.serializeToString(ConfigManager.getInstance().getConfig()));

        // And make assertions about the current matching state
        assertEquals(3, vsm.getVsmState().allConnectedCameras.size());
        assertEquals(2, vsm.getVsmState().disabledConfigs.size());
        assertEquals(1, vsm.vmm.getModules().size());
    }
}
