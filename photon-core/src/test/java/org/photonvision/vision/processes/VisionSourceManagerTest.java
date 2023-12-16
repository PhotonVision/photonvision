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

import edu.wpi.first.cscore.UsbCameraInfo;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.configuration.ConfigManager;

public class VisionSourceManagerTest {
    @Test
    public void visionSourceTest() {
        var inst = new VisionSourceManager();
        var infoList = new ArrayList<UsbCameraInfo>();
        inst.cameraInfoSupplier = () -> infoList;
        ConfigManager.getInstance().load();

        inst.tryMatchUSBCamImpl();

        var config3 =
                new CameraConfiguration(
                        "secondTestVideo",
                        "secondTestVideo1",
                        "secondTestVideo1",
                        "dev/video1",
                        new String[] {"by-id/123"});
        var config4 =
                new CameraConfiguration(
                        "secondTestVideo",
                        "secondTestVideo2",
                        "secondTestVideo2",
                        "dev/video2",
                        new String[] {"by-id/321"});

        UsbCameraInfo info1 = new UsbCameraInfo(0, "dev/video0", "testVideo", new String[0], 1, 2);

        infoList.add(info1);

        inst.registerLoadedConfigs(config3, config4);

        inst.tryMatchUSBCamImpl(false);

        assertTrue(inst.knownUsbCameras.contains(info1));
        assertEquals(2, inst.unmatchedLoadedConfigs.size());

        UsbCameraInfo info2 = new UsbCameraInfo(0, "dev/video1", "testVideo", new String[0], 1, 2);
      
        infoList.add(info2);

        var cams = inst.matchUSBCameras(infoList, inst.unmatchedLoadedConfigs);

        // assertEquals("testVideo (1)", cams.get(0).uniqueName); // Proper suffixing

        inst.tryMatchUSBCamImpl(false);

        assertTrue(inst.knownUsbCameras.contains(info2));
        assertEquals(2, inst.unmatchedLoadedConfigs.size());

        UsbCameraInfo info3 =
                new UsbCameraInfo(0, "dev/video2", "secondTestVideo", new String[] {"by-id/123"}, 2, 1);

        UsbCameraInfo info4 =
                new UsbCameraInfo(0, "dev/video3", "secondTestVideo", new String[] {"by-id/321"}, 3, 1);

        infoList.add(info4);

        cams = inst.matchUSBCameras(infoList, inst.unmatchedLoadedConfigs);

        var cam4 =
                cams.stream()
                        .filter(
                                cam -> cam.otherPaths.length > 0 && cam.otherPaths[0].equals(config4.otherPaths[0]))
                        .findFirst()
                        .orElse(null);
        // If this is null, cam4 got matched to config3 instead of config4

        assertEquals(cam4.nickname, config4.nickname);

        infoList.add(info3);

        cams = inst.matchUSBCameras(infoList, inst.unmatchedLoadedConfigs);

        inst.tryMatchUSBCamImpl(false);

        assertTrue(inst.knownUsbCameras.contains(info2));
        assertTrue(inst.knownUsbCameras.contains(info3));

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
        assertEquals(4, inst.knownUsbCameras.size());
        assertEquals(0, inst.unmatchedLoadedConfigs.size());

        UsbCameraInfo info3 =
                new UsbCameraInfo(
                        2,
                        "/dev/video2",
                        "Left Camera",
                        new String[] {
                            "/dev/v4l/by-id/usb-Arducam_Technology_Co.__Ltd._Left_Camera_12345-video-index0",
                            "/dev/v4l/by-path/platform-xhci-hcd.0-usb-0:2:1.0-video-index0"
                        },
                        3,
                        4);
        infoList.add(info3);
        inst.tryMatchUSBCamImpl(false);

        assertTrue(inst.knownUsbCameras.contains(info3));
        assertEquals(3, inst.knownUsbCameras.size());
        assertEquals(0, inst.unmatchedLoadedConfigs.size());

        UsbCameraInfo info4 =
                new UsbCameraInfo(
                        3,
                        "dev/video3",
                        "Right Camera",
                        new String[] {
                            "/dev/v4l/by-id/usb-Arducam_Technology_Co.__Ltd._Right_Camera_123456-video-index0",
                            "/dev/v4l/by-path/platform-xhci-hcd.1-usb-0:1:1.0-video-index0"
                        },
                        5,
                        6);
        infoList.add(info4);
        inst.tryMatchUSBCamImpl(false);

        assertTrue(inst.knownUsbCameras.contains(info4));
        assertEquals(4, inst.knownUsbCameras.size());
        assertEquals(0, inst.unmatchedLoadedConfigs.size());

        // RPI 5 CSI Tests

        UsbCameraInfo info5 =
                new UsbCameraInfo(
                        4,
                        "dev/video4",
                        "CSICAM-DEV", // Typically rp1-cfe for unit test changed to CSICAM-DEV
                        new String[] {"/dev/v4l/by-path/platform-1f00110000.csi-video-index0"},
                        7,
                        8);
        infoList.add(info5);
        inst.tryMatchUSBCamImpl(false);

        assertTrue(inst.knownUsbCameras.contains(info5));
        assertEquals(5, inst.knownUsbCameras.size());
        assertEquals(0, inst.unmatchedLoadedConfigs.size());

        UsbCameraInfo info6 =
                new UsbCameraInfo(
                        5,
                        "dev/video8",
                        "CSICAM-DEV", // Typically rp1-cfe for unit test changed to CSICAM-DEV
                        new String[] {"/dev/v4l/by-path/platform-1f00110000.csi-video-index4"},
                        9,
                        10);
        infoList.add(info6);
        inst.tryMatchUSBCamImpl(false);

        assertTrue(!inst.knownUsbCameras.contains(info6)); // This camera should not be recognized/used.
        assertEquals(5, inst.knownUsbCameras.size());
        assertEquals(0, inst.unmatchedLoadedConfigs.size());

        UsbCameraInfo info7 =
                new UsbCameraInfo(
                        6,
                        "dev/video9",
                        "CSICAM-DEV", // Typically rp1-cfe for unit test changed to CSICAM-DEV
                        new String[] {"/dev/v4l/by-path/platform-1f00110000.csi-video-index5"},
                        11,
                        12);
        infoList.add(info7);
        inst.tryMatchUSBCamImpl(false);

        assertTrue(!inst.knownUsbCameras.contains(info7)); // This camera should not be recognized/used.
        assertEquals(5, inst.knownUsbCameras.size());
        assertEquals(0, inst.unmatchedLoadedConfigs.size());
    }
}
