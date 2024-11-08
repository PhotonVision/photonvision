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

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.LogLevel;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.camera.CameraInfo;
import org.photonvision.vision.camera.CameraType;

public class VisionSourceManagerTest {
    @Test
    public void visionSourceTest() {
        Logger.setLevel(LogGroup.Camera, LogLevel.DEBUG);

        var inst = new VisionSourceManager();
        var cameraInfos = new ArrayList<CameraInfo>();
        ConfigManager.getInstance().clearConfig();
        ConfigManager.getInstance().load();

        inst.tryMatchCamImpl(cameraInfos);

        var config3 =
                new CameraConfiguration(
                        "thirdTestVideo",
                        "thirdTestVideo",
                        "thirdTestVideo",
                        "dev/video1",
                        new String[] {"by-id/123"});
        config3.usbVID = 3;
        config3.usbPID = 4;
        var config4 =
                new CameraConfiguration(
                        "fourthTestVideo",
                        "fourthTestVideo",
                        "fourthTestVideo",
                        "dev/video2",
                        new String[] {"by-id/321"});
        config4.usbVID = 5;
        config4.usbPID = 6;

        CameraInfo info1 =
                new CameraInfo(0, "dev/video0", "testVideo", new String[] {"/usb/path/0"}, 1, 2);

        cameraInfos.add(info1);

        inst.registerLoadedConfigs(config3, config4);

        inst.tryMatchCamImpl(cameraInfos);

        assertTrue(inst.knownCameras.contains(info1));
        assertEquals(2, inst.unmatchedLoadedConfigs.size());

        CameraInfo info2 =
                new CameraInfo(0, "dev/video1", "secondTestVideo", new String[] {"/usb/path/1"}, 2, 3);

        cameraInfos.add(info2);

        var cams = inst.matchCameras(cameraInfos, inst.unmatchedLoadedConfigs);

        // assertEquals("testVideo (1)", cams.get(0).uniqueName); // Proper suffixing

        inst.tryMatchCamImpl(cameraInfos);

        assertTrue(inst.knownCameras.contains(info2));
        assertEquals(2, inst.unmatchedLoadedConfigs.size());

        CameraInfo info3 =
                new CameraInfo(0, "dev/video2", "thirdTestVideo", new String[] {"by-id/123"}, 3, 4);

        CameraInfo info4 =
                new CameraInfo(0, "dev/video3", "fourthTestVideo", new String[] {"by-id/321"}, 5, 6);

        cameraInfos.add(info4);

        cams = inst.matchCameras(cameraInfos, inst.unmatchedLoadedConfigs);

        var cam4 =
                cams.stream()
                        .filter(
                                cam -> cam.otherPaths.length > 0 && cam.otherPaths[0].equals(config4.otherPaths[0]))
                        .findFirst()
                        .orElse(null);
        // If this is null, cam4 got matched to config3 instead of config4

        assertEquals(cam4.nickname, config4.nickname);

        cameraInfos.add(info3);

        cams = inst.matchCameras(cameraInfos, inst.unmatchedLoadedConfigs);

        inst.tryMatchCamImpl(cameraInfos);

        assertTrue(inst.knownCameras.contains(info2));
        assertTrue(inst.knownCameras.contains(info3));

        var cam3 =
                cams.stream()
                        .filter(
                                cam -> cam.otherPaths.length > 0 && cam.otherPaths[0].equals(config3.otherPaths[0]))
                        .findFirst()
                        .orElse(null);
        cam4 =
                cams.stream()
                        .filter(
                                cam -> cam.otherPaths.length > 0 && cam.otherPaths[0].equals(config4.otherPaths[0]))
                        .findFirst()
                        .orElse(null);

        assertEquals(cam3.nickname, config3.nickname);
        assertEquals(cam4.nickname, config4.nickname);

        CameraInfo info5 =
                new CameraInfo(
                        2,
                        "/dev/video2",
                        "Left Camera",
                        new String[] {
                            "/dev/v4l/by-id/usb-Arducam_Technology_Co.__Ltd._Left_Camera_12345-video-index0",
                            "/dev/v4l/by-path/platform-xhci-hcd.0-usb-0:2:1.0-video-index0"
                        },
                        7,
                        8);
        cameraInfos.add(info5);
        inst.tryMatchCamImpl(cameraInfos);

        assertTrue(inst.knownCameras.contains(info5));

        CameraInfo info6 =
                new CameraInfo(
                        3,
                        "dev/video3",
                        "Right Camera",
                        new String[] {
                            "/dev/v4l/by-id/usb-Arducam_Technology_Co.__Ltd._Right_Camera_123456-video-index0",
                            "/dev/v4l/by-path/platform-xhci-hcd.1-usb-0:1:1.0-video-index0"
                        },
                        9,
                        10);
        cameraInfos.add(info6);
        inst.tryMatchCamImpl(cameraInfos);

        assertTrue(inst.knownCameras.contains(info6));

        // RPI 5 CSI Tests

        // CSI CAMERAS SHOULD NOT BE LOADED LIKE THIS THEY SHOULD GO THROUGH LIBCAM.
        CameraInfo info7 =
                new CameraInfo(
                        4,
                        "dev/video4",
                        "CSICAM-DEV", // Typically rp1-cfe for unit test changed to CSICAM-DEV
                        new String[] {"/dev/v4l/by-path/platform-1f00110000.csi-video-index0"},
                        11,
                        12);
        cameraInfos.add(info7);
        inst.tryMatchCamImpl(cameraInfos);

        assertTrue(!inst.knownCameras.contains(info7)); // This camera should not be recognized/used.

        CameraInfo info8 =
                new CameraInfo(
                        5,
                        "dev/video8",
                        "CSICAM-DEV", // Typically rp1-cfe for unit test changed to CSICAM-DEV
                        new String[] {"/dev/v4l/by-path/platform-1f00110000.csi-video-index4"},
                        13,
                        14);
        cameraInfos.add(info8);
        inst.tryMatchCamImpl(cameraInfos);

        assertTrue(!inst.knownCameras.contains(info8)); // This camera should not be recognized/used.

        CameraInfo info9 =
                new CameraInfo(
                        6,
                        "dev/video9",
                        "CSICAM-DEV", // Typically rp1-cfe for unit test changed to CSICAM-DEV
                        new String[] {"/dev/v4l/by-path/platform-1f00110000.csi-video-index5"},
                        15,
                        16);
        cameraInfos.add(info9);
        inst.tryMatchCamImpl(cameraInfos);

        assertTrue(!inst.knownCameras.contains(info9)); // This camera should not be recognized/used.
        assertEquals(6, inst.knownCameras.size());
        assertEquals(0, inst.unmatchedLoadedConfigs.size());

        // RPI LIBCAMERA CSI CAMERA TESTS
        CameraInfo info10 =
                new CameraInfo(
                        -1,
                        "/base/soc/i2c0mux/i2c@0/ov9281@60",
                        "OV9281", // Typically rp1-cfe for unit test changed to CSICAM-DEV
                        new String[] {},
                        -1,
                        -1,
                        CameraType.ZeroCopyPicam);
        cameraInfos.add(info10);
        inst.tryMatchCamImpl(cameraInfos);

        assertTrue(inst.knownCameras.contains(info10));
        assertEquals(7, inst.knownCameras.size());
        assertEquals(0, inst.unmatchedLoadedConfigs.size());

        CameraInfo info11 =
                new CameraInfo(
                        -1,
                        "/base/soc/i2c0mux/i2c@1/ov9281@60",
                        "OV9281", // Typically rp1-cfe for unit test changed to CSICAM-DEV
                        new String[] {},
                        -1,
                        -1,
                        CameraType.ZeroCopyPicam);
        cameraInfos.add(info11);
        inst.tryMatchCamImpl(cameraInfos);

        assertTrue(inst.knownCameras.contains(info11));
        assertEquals(8, inst.knownCameras.size());
        assertEquals(0, inst.unmatchedLoadedConfigs.size());

        CameraInfo info12 =
                new CameraInfo(
                        -1,
                        " /base/axi/pcie@120000/rp1/i2c@80000/ov5647@36",
                        "Camera Module v1",
                        new String[] {},
                        -1,
                        -1,
                        CameraType.ZeroCopyPicam);
        cameraInfos.add(info12);
        inst.tryMatchCamImpl(cameraInfos);

        assertTrue(inst.knownCameras.contains(info12));
        assertEquals(9, inst.knownCameras.size());
        assertEquals(0, inst.unmatchedLoadedConfigs.size());

        CameraInfo info13 =
                new CameraInfo(
                        -1,
                        "/base/axi/pcie@120000/rp1/i2c@88000/imx708@1a",
                        "Camera Module v3",
                        new String[] {},
                        -1,
                        -1,
                        CameraType.ZeroCopyPicam);
        cameraInfos.add(info13);
        inst.tryMatchCamImpl(cameraInfos);

        assertTrue(inst.knownCameras.contains(info13));
        assertEquals(10, inst.knownCameras.size());
        assertEquals(0, inst.unmatchedLoadedConfigs.size());
    }

    @Test
    public void testDisableInhibitPathChangeIdenticalCams() {
        Logger.setLevel(LogGroup.Camera, LogLevel.DEBUG);

        var inst = new VisionSourceManager();
        ConfigManager.getInstance().clearConfig();
        ConfigManager.getInstance().load();
        ConfigManager.getInstance().getConfig().getNetworkConfig().matchCamerasOnlyByPath = false;

        var CAM2_OLD_PATH =
                new String[] {"/dev/v4l/by-path/platform-fc880000.usb-usb-0:1:1.0-video-index0"};
        var CAM2_NEW_PATH =
                new String[] {"/dev/v4l/by-path/platform-fc880080.usb-usb-0:1:1.3-video-index0"};

        var CAM1_OLD_PATHS =
                new String[] {
                    "/dev/v4l/by-id/usb-Arducam_Technology_Co.__Ltd._Arducam_OV2311_USB_Camera_UC621-video-index0",
                    "/dev/v4l/by-path/platform-fc800000.usb-usb-0:1:1.0-video-index0"
                };

        var camera1_saved_config =
                new CameraConfiguration(
                        "Arducam OV2311 USB Camera",
                        "Arducam OV2311 USB Camera",
                        "front-left",
                        "/dev/video0",
                        CAM1_OLD_PATHS);
        camera1_saved_config.usbVID = 3141;
        camera1_saved_config.usbPID = 25446;
        var camera2_saved_config =
                new CameraConfiguration(
                        "Arducam OV2311 USB Camera",
                        "Arducam OV2311 USB Camera (1)",
                        "front-left",
                        "/dev/video2",
                        CAM2_OLD_PATH);
        camera2_saved_config.usbVID = 3141;
        camera2_saved_config.usbPID = 25446;

        // And load our "old" configs
        inst.registerLoadedConfigs(camera1_saved_config, camera2_saved_config);

        // Camera attached to new port, but strict matching disabled
        {
            CameraInfo info1 =
                    new CameraInfo(
                            0, "/dev/video11", "Arducam OV2311 USB Camera", CAM1_OLD_PATHS, 3141, 25446);
            CameraInfo info2 =
                    new CameraInfo(
                            0, "/dev/video12", "Arducam OV2311 USB Camera", CAM2_NEW_PATH, 3141, 25446);

            var cameraInfos = new ArrayList<CameraInfo>();
            cameraInfos.add(info1);
            cameraInfos.add(info2);
            List<VisionSource> ret1 = inst.tryMatchCamImpl(cameraInfos);

            // and check the new one got matched got matched
            assertEquals(2, ret1.size());
            assertEquals(
                    1, ret1.stream().filter(it -> it.cameraConfiguration.path.equals(info1.path)).count());
            assertEquals(
                    1, ret1.stream().filter(it -> it.cameraConfiguration.path.equals(info2.path)).count());
        }
    }

    @Test
    public void testInhibitPathChangeIdenticalCams() {
        Logger.setLevel(LogGroup.Camera, LogLevel.DEBUG);

        var inst = new VisionSourceManager();
        ConfigManager.getInstance().clearConfig();
        ConfigManager.getInstance().load();
        ConfigManager.getInstance().getConfig().getNetworkConfig().matchCamerasOnlyByPath = true;

        var CAM2_OLD_PATH =
                new String[] {"/dev/v4l/by-path/platform-fc880000.usb-usb-0:1:1.0-video-index0"};
        var CAM2_NEW_PATH =
                new String[] {"/dev/v4l/by-path/platform-fc880080.usb-usb-0:1:1.3-video-index0"};

        var CAM1_OLD_PATHS =
                new String[] {
                    "/dev/v4l/by-id/usb-Arducam_Technology_Co.__Ltd._Arducam_OV2311_USB_Camera_UC621-video-index0",
                    "/dev/v4l/by-path/platform-fc800000.usb-usb-0:1:1.0-video-index0"
                };

        var camera1_saved_config =
                new CameraConfiguration(
                        "Arducam OV2311 USB Camera",
                        "Arducam OV2311 USB Camera (1)",
                        "front-left",
                        "/dev/video0",
                        CAM1_OLD_PATHS);
        camera1_saved_config.usbVID = 3141;
        camera1_saved_config.usbPID = 25446;
        var camera2_saved_config =
                new CameraConfiguration(
                        "Arducam OV2311 USB Camera",
                        "Arducam OV2311 USB Camera (1)",
                        "front-left",
                        "/dev/video2",
                        CAM2_OLD_PATH);
        camera2_saved_config.usbVID = 3141;
        camera2_saved_config.usbPID = 25446;

        // And load our "old" configs
        inst.registerLoadedConfigs(camera1_saved_config, camera2_saved_config);

        // initial pass with camera in the wrong spot
        {
            // Give our cameras new "paths" to fake the windows logic out. this should not
            // affect strict matching
            CameraInfo info1 =
                    new CameraInfo(
                            0, "/dev/video11", "Arducam OV2311 USB Camera", CAM1_OLD_PATHS, 3141, 25446);
            CameraInfo info2 =
                    new CameraInfo(
                            0, "/dev/video12", "Arducam OV2311 USB Camera", CAM2_NEW_PATH, 3141, 25446);

            var cameraInfos = new ArrayList<CameraInfo>();
            cameraInfos.add(info1);
            cameraInfos.add(info2);
            List<VisionSource> ret1 = inst.tryMatchCamImpl(cameraInfos);

            // Our cameras should be "known"
            assertTrue(inst.knownCameras.contains(info1));
            assertTrue(inst.knownCameras.contains(info2));
            assertEquals(2, inst.knownCameras.size());

            // And we should have matched one camera
            assertEquals(1, ret1.size());
            // and only matched camera1, not 2
            assertEquals(
                    1, ret1.stream().filter(it -> it.cameraConfiguration.path.equals(info1.path)).count());
            assertEquals(
                    0, ret1.stream().filter(it -> it.cameraConfiguration.path.equals(info2.path)).count());
        }

        // Now move our camera back
        {
            CameraInfo info1 =
                    new CameraInfo(
                            0, "/dev/video11", "Arducam OV2311 USB Camera", CAM1_OLD_PATHS, 3141, 25446);
            CameraInfo info2 =
                    new CameraInfo(
                            0, "/dev/video12", "Arducam OV2311 USB Camera", CAM2_OLD_PATH, 3141, 25446);

            var cameraInfos = new ArrayList<CameraInfo>();
            cameraInfos.add(info1);
            cameraInfos.add(info2);
            List<VisionSource> ret1 = inst.tryMatchCamImpl(cameraInfos);

            // and check the new one got matched got matched
            assertEquals(1, ret1.size());
            assertEquals(
                    0, ret1.stream().filter(it -> it.cameraConfiguration.path.equals(info1.path)).count());
            assertEquals(
                    1, ret1.stream().filter(it -> it.cameraConfiguration.path.equals(info2.path)).count());
        }
    }

    @Test
    public void testCSICameraMatching() {
        Logger.setLevel(LogGroup.Camera, LogLevel.DEBUG);

        // List of known cameras
        var cameraInfos = new ArrayList<CameraInfo>();

        var inst = new VisionSourceManager();
        ConfigManager.getInstance().clearConfig();
        ConfigManager.getInstance().load();
        ConfigManager.getInstance().getConfig().getNetworkConfig().matchCamerasOnlyByPath = false;

        CameraInfo info1 =
                new CameraInfo(
                        -1,
                        "/base/soc/i2c0mux/i2c@0/ov9281@60",
                        "OV9281", // Typically rp1-cfe for unit test changed to CSICAM-DEV
                        new String[] {},
                        -1,
                        -1,
                        CameraType.ZeroCopyPicam);

        CameraInfo info2 =
                new CameraInfo(
                        -1,
                        "/base/soc/i2c0mux/i2c@1/ov9281@60",
                        "OV9281", // Typically rp1-cfe for unit test changed to CSICAM-DEV
                        new String[] {},
                        -1,
                        -1,
                        CameraType.ZeroCopyPicam);

        var camera1_saved_config =
                new CameraConfiguration(
                        "OV9281", "OV9281", "test-1", "/base/soc/i2c0mux/i2c@0/ov9281@60", new String[0]);
        camera1_saved_config.cameraType = CameraType.ZeroCopyPicam;
        camera1_saved_config.usbVID = -1;
        camera1_saved_config.usbPID = -1;

        var camera2_saved_config =
                new CameraConfiguration(
                        "OV9281", "OV9281 (1)", "test-2", "/base/soc/i2c0mux/i2c@1/ov9281@60", new String[0]);
        camera2_saved_config.usbVID = -1;
        camera2_saved_config.usbPID = -1;
        camera2_saved_config.cameraType = CameraType.ZeroCopyPicam;

        cameraInfos.add(info1);
        cameraInfos.add(info2);

        // Try matching with both cameras being "known"
        inst.registerLoadedConfigs(camera1_saved_config, camera2_saved_config);
        var ret1 = inst.tryMatchCamImpl(cameraInfos);

        // Our cameras should be "known"
        assertTrue(inst.knownCameras.contains(info1));
        assertTrue(inst.knownCameras.contains(info2));
        assertEquals(2, inst.knownCameras.size());
        assertEquals(2, ret1.size());

        // Exactly one camera should have the path we put in
        for (int i = 0; i < cameraInfos.size(); i++) {
            var testPath = cameraInfos.get(i).path;
            assertEquals(
                    1, ret1.stream().filter(it -> testPath.equals(it.cameraConfiguration.path)).count());
        }
    }

    @Test
    public void testNoOtherPaths() {
        Logger.setLevel(LogGroup.Camera, LogLevel.DEBUG);

        // List of known cameras
        var cameraInfos = new ArrayList<CameraInfo>();

        var inst = new VisionSourceManager();
        ConfigManager.getInstance().clearConfig();
        ConfigManager.getInstance().load();
        ConfigManager.getInstance().getConfig().getNetworkConfig().matchCamerasOnlyByPath = false;

        // Match empty camera infos
        inst.tryMatchCamImpl(cameraInfos);

        CameraInfo info1 =
                new CameraInfo(0, "/dev/video0", "Arducam OV2311 USB Camera", new String[] {}, 3141, 25446);

        cameraInfos.add(info1);

        // Match two "new" cameras
        var ret1 = inst.tryMatchCamImpl(cameraInfos, Platform.LINUX_64);

        // Our cameras should be "known"
        assertFalse(inst.knownCameras.contains(info1));
        assertEquals(0, inst.knownCameras.size());
        assertEquals(null, ret1);

        // Match two "new" cameras
        var ret2 = inst.tryMatchCamImpl(cameraInfos, Platform.WINDOWS_64);

        // Our cameras should be "known"
        assertTrue(inst.knownCameras.contains(info1));
        assertEquals(1, inst.knownCameras.size());
        assertEquals(1, ret2.size());
    }

    @Test
    public void testIdenticalCameras() {
        Logger.setLevel(LogGroup.Camera, LogLevel.DEBUG);

        // List of known cameras
        var cameraInfos = new ArrayList<CameraInfo>();

        var inst = new VisionSourceManager();
        ConfigManager.getInstance().clearConfig();
        ConfigManager.getInstance().load();
        ConfigManager.getInstance().getConfig().getNetworkConfig().matchCamerasOnlyByPath = false;

        // Match empty camera infos
        inst.tryMatchCamImpl(cameraInfos);

        CameraInfo info1 =
                new CameraInfo(
                        0,
                        "/dev/video0",
                        "Arducam OV2311 USB Camera",
                        new String[] {
                            "/dev/v4l/by-path/platform-fc800000.usb-usb-0:1:1.0-video-index0" // V4l doesnt assign
                            // by-id paths that
                            // are identical to
                            // two different
                            // cameras
                        },
                        3141,
                        25446);
        CameraInfo info2 =
                new CameraInfo(
                        0,
                        "/dev/video2",
                        "Arducam OV2311 USB Camera",
                        new String[] {
                            "/dev/v4l/by-id/usb-Arducam_Technology_Co.__Ltd._Arducam_OV2311_USB_Camera_UC621-video-index0",
                            "/dev/v4l/by-path/platform-fc880000.usb-usb-0:1:1.0-video-index0"
                        },
                        3141,
                        25446);

        cameraInfos.add(info1);
        cameraInfos.add(info2);

        // Match two "new" cameras
        var ret1 = inst.tryMatchCamImpl(cameraInfos);

        // Our cameras should be "known"
        assertTrue(inst.knownCameras.contains(info1));
        assertTrue(inst.knownCameras.contains(info2));
        assertEquals(2, inst.knownCameras.size());
        assertEquals(2, ret1.size());

        // Exactly one camera should have the path we put in
        for (int i = 0; i < cameraInfos.size(); i++) {
            var testPath = cameraInfos.get(i).getUSBPath().get();
            assertEquals(
                    1,
                    ret1.stream()
                            .filter(it -> testPath.equals(it.cameraConfiguration.getUSBPath().get()))
                            .count());
        }

        // and the names should be unique
        for (int i = 0; i < ret1.size(); i++) {
            var thisName = ret1.get(i).cameraConfiguration.uniqueName;
            assertEquals(
                    1,
                    ret1.stream().filter(it -> thisName.equals(it.cameraConfiguration.uniqueName)).count());
        }

        // duplicate cameras, same info, new ref
        var duplicateCameraInfos = new ArrayList<CameraInfo>();
        CameraInfo info1_dup =
                new CameraInfo(
                        0,
                        "/dev/video0",
                        "Arducam OV2311 USB Camera",
                        new String[] {
                            "/dev/v4l/by-path/platform-fc800000.usb-usb-0:1:1.0-video-index0" // V4l doesnt assign
                            // by-id paths that
                            // are identical to
                            // two different
                            // cameras
                        },
                        3141,
                        25446);
        CameraInfo info2_dup =
                new CameraInfo(
                        0,
                        "/dev/video2",
                        "Arducam OV2311 USB Camera",
                        new String[] {
                            "/dev/v4l/by-id/usb-Arducam_Technology_Co.__Ltd._Arducam_OV2311_USB_Camera_UC621-video-index0",
                            "/dev/v4l/by-path/platform-fc880000.usb-usb-0:1:1.0-video-index0"
                        },
                        3141,
                        25446);

        duplicateCameraInfos.add(info1_dup);
        duplicateCameraInfos.add(info2_dup);

        inst.tryMatchCamImpl(duplicateCameraInfos);

        // Our cameras should be "known", and we should only "know" two cameras still
        assertTrue(inst.knownCameras.contains(info1_dup));
        assertTrue(inst.knownCameras.contains(info2_dup));
        assertEquals(2, inst.knownCameras.size());

        // duplicate cameras this simulates unplugging one and plugging the other in where v4l assigns
        // the same by-id path to the other camera
        var duplicateCameraInfos1 = new ArrayList<CameraInfo>();
        CameraInfo info3_dup =
                new CameraInfo(
                        0,
                        "/dev/video0",
                        "Arducam OV2311 USB Camera",
                        new String[] {
                            "/dev/v4l/by-id/usb-Arducam_Technology_Co.__Ltd._Arducam_OV2311_USB_Camera_UC621-video-index0",
                            "/dev/v4l/by-path/platform-fc800000.usb-usb-0:1:1.0-video-index0"
                        },
                        3141,
                        25446);
        CameraInfo info4_dup =
                new CameraInfo(
                        0,
                        "/dev/video2",
                        "Arducam OV2311 USB Camera",
                        new String[] {
                            "/dev/v4l/by-path/platform-fc880000.usb-usb-0:1:1.0-video-index0" // V4l doesnt assign
                            // by-id paths that
                            // are identical to
                            // two different
                            // cameras
                        },
                        3141,
                        25446);

        duplicateCameraInfos1.add(info3_dup);
        duplicateCameraInfos1.add(info4_dup);

        inst.tryMatchCamImpl(duplicateCameraInfos1);

        // Our cameras should be "known", and we should only "know" two cameras still
        assertTrue(inst.knownCameras.contains(info3_dup));
        assertTrue(inst.knownCameras.contains(info4_dup));
        assertEquals(2, inst.knownCameras.size());
    }
}
