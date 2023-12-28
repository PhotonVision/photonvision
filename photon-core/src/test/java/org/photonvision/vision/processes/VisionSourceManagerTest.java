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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.vision.camera.CameraInfo;
import org.photonvision.vision.camera.CameraType;

public class VisionSourceManagerTest {
    @Test
    public void visionSourceTest() {
        var inst = new VisionSourceManager();
        var cameraInfos = new ArrayList<CameraInfo>();
        ConfigManager.getInstance().load();

        inst.tryMatchCamImpl(cameraInfos);

        var config3 =
                new CameraConfiguration(
                        "thirdTestVideo",
                        "thirdTestVideo",
                        "thirdTestVideo",
                        "dev/video1",
                        new String[] {"by-id/123"});
        var config4 =
                new CameraConfiguration(
                        "fourthTestVideo",
                        "fourthTestVideo",
                        "fourthTestVideo",
                        "dev/video2",
                        new String[] {"by-id/321"});

        CameraInfo info1 = new CameraInfo(0, "dev/video0", "testVideo", new String[0], 1, 2);

        cameraInfos.add(info1);

        inst.registerLoadedConfigs(config3, config4);

        inst.tryMatchCamImpl(cameraInfos);

        assertTrue(inst.knownCameras.contains(info1));
        assertEquals(2, inst.unmatchedLoadedConfigs.size());

        CameraInfo info2 = new CameraInfo(0, "dev/video1", "secondTestVideo", new String[0], 2, 3);

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
}
