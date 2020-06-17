package com.chameleonvision.common.vision.processes;

import com.chameleonvision.common.configuration.CameraConfiguration;
import com.chameleonvision.common.util.TestUtils;
import com.chameleonvision.common.vision.camera.USBCameraSource;
import edu.wpi.cscore.UsbCameraInfo;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        VisionSourceManager visionSourceManager = new VisionSourceManager();
        List<VisionSource> i = visionSourceManager.LoadAllSources(camConfig, usbCameraInfos);
        for (var source : i) {
            Assertions.assertEquals(source, usbCameraSources.get(i.indexOf(source)));
        }
    }
}
