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

import edu.wpi.cscore.UsbCameraInfo;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.camera.USBCameraSource;

public class VisionSourceManagerTest {
    @BeforeEach
    public void init() {
        TestUtils.loadLibraries();
    }

    final List<UsbCameraInfo> usbCameraInfos =
            List.of(
                    new UsbCameraInfo(0, "/this-is-a-real-path", "cameraByPath", new String[] {""}, 1, 1),
                    new UsbCameraInfo(2, "/this-is-a-fake-path1", "cameraById", new String[] {""}, 420, 1),
                    new UsbCameraInfo(1, "/this-is-a-real-path2", "cameraByPath", new String[] {""}, 1, 1),
                    new UsbCameraInfo(3, "/this-is-a-fake-path2", "cameraById", new String[] {""}, 420, 1),
                    new UsbCameraInfo(4, "/fake-path420", "notExisting", new String[] {""}, 420, 1),
                    new UsbCameraInfo(5, "/fake-path421", "notExisting", new String[] {""}, 420, 1));

    final List<CameraConfiguration> camConfig =
            List.of(
                    new CameraConfiguration("cameraByPath", "dank meme", "good name", "/this-is-a-real-path"),
                    new CameraConfiguration(
                            "cameraByPath", "dank meme2", "very original", "/this-is-a-real-path2"),
                    new CameraConfiguration("cameraById", "camera", "my camera", "2"),
                    new CameraConfiguration("cameraById", "camera2", "my camera", "3"));

    final List<USBCameraSource> usbCameraSources =
            List.of(
                    new USBCameraSource(camConfig.get(0)),
                    new USBCameraSource(camConfig.get(1)),
                    new USBCameraSource(camConfig.get(2)),
                    new USBCameraSource(camConfig.get(3)),
                    new USBCameraSource(
                            new CameraConfiguration("notExisting", "notExisting", "notExisting", "4")),
                    new USBCameraSource(
                            new CameraConfiguration("notExisting", "notExisting (1)", "notExisting (1)", "5")));

    @Test
    public void visionSourceTest() {
        List<VisionSource> i = VisionSourceManager.loadAllSources(camConfig, usbCameraInfos);
        for (var source : i) {
            Assertions.assertEquals(source, usbCameraSources.get(i.indexOf(source)));
        }
    }
}
