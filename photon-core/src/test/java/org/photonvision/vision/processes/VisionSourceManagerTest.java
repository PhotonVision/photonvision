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

import edu.wpi.first.cscore.UsbCameraInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.util.TestUtils;
import org.photonvision.common.util.file.JacksonUtils;
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
            for (var module : this.getVisionModules()) {
                // release native resources
                module.stop();
            }
        }
    }

    @BeforeAll
    public static void setup() {
        // Broadcast all still calls into configmanager (ew) so set that up here
        ConfigManager.getInstance().load();

        // We also need to load cscore since we call into it.
        TestUtils.loadLibraries();
    }

    @Test
    public void testCameraInfoSerde() throws InterruptedException, IOException {
        // var vsm = new VisionSourceManager();
        // for (int i = 0; i < 100; i++) {
        // Thread.sleep(1000);
        // System.out.println(vsm.getConnectedCameras());
        // }

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
        // assertEquals(0, vsm.getVsmState().activeCameras.size());
        // assertEquals(0, vsm.getVsmState().disabledCameras.size());
        assertEquals(0, vsm.getVsmState().allConnectedCameras.size());
    }

    // @Test
    // public void testEnabledDisabled() {
    //     // GIVEN a VSM
    //     var vsm = new TestVsm();
    //     // AND one enabled camera, and one disabled camera
    //     var enabledCam =
    //             new CameraConfiguration(
    //                     "Lifecam HD-3000",
    //                     "Lifecam HD-3000",
    //                     "Matt's Awesome Camera",
    //                     "/dev/video0",
    //                     new String[] {"/dev/v4l/by-path/foobar1"});
    //     var disabledCam =
    //             new CameraConfiguration(
    //                     "Lifecam HD-3000",
    //                     "Lifecam HD-3000 (1)",
    //                     "Another Awesome Camera",
    //                     "/dev/video1",
    //                     new String[] {"/dev/v4l/by-path/foobar2"});
    //     disabledCam.deactivated = true;
    //     vsm.testCameras =
    //             List.of(
    //                     PVCameraInfo.fromUsbCameraInfo(
    //                             new UsbCameraInfo(
    //                                     0,
    //                                     enabledCam.path,
    //                                     enabledCam.baseName,
    //                                     enabledCam.otherPaths,
    //                                     enabledCam.usbVID,
    //                                     enabledCam.usbPID)),
    //                     PVCameraInfo.fromUsbCameraInfo(
    //                             new UsbCameraInfo(
    //                                     1,
    //                                     disabledCam.path,
    //                                     disabledCam.baseName,
    //                                     disabledCam.otherPaths,
    //                                     disabledCam.usbVID,
    //                                     disabledCam.usbPID)));
    //     // WHEN cameras are loaded from disk
    //     vsm.registerLoadedConfigs(List.of(enabledCam, disabledCam));
    //     vsm.discoverNewDevices();
    //     // the enabled and disabled cameras will be matched
    //     assertEquals(1, vsm.getVsmState().activeCameras.size());
    //     assertEquals(1, vsm.getVsmState().disabledCameras.size());
    //     assertEquals(2, vsm.getVsmState().allConnectedCameras.size());

    //     vsm.teardown();
    // }
}
