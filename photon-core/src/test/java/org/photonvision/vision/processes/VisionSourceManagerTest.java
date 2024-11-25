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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.camera.PVCameraDevice;
import org.photonvision.vision.frame.FrameProvider;

import edu.wpi.first.cscore.CameraServerJNI;
import edu.wpi.first.cscore.UsbCameraInfo;

public class VisionSourceManagerTest {
    // Test harness that overrides getConnectedCameras, but uses USB cameras for
    // everything else
    // when we start testing libcamera stuff we'll need to mock more stuff out
    private static class TestVsm extends VisionSourceManager {
        public List<PVCameraDevice> testCameras = new ArrayList<>();

        @Override
        protected List<PVCameraDevice> getConnectedCameras() {
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
    public void testEmpty() {
        var vsm = new TestVsm();

        List<CameraConfiguration> configs = List.of();
        vsm.registerLoadedConfigs(configs);

        // instead of registering a timed task, tick ourselves
        vsm.discoverNewDevices();

        // And make assertions about the current matching state
        assertEquals(0, vsm.getVsmState().activeCameras.size());
        assertEquals(0, vsm.getVsmState().disabledCameras.size());
        assertEquals(0, vsm.getVsmState().allConnectedCameras.size());
    }

    @Test
    public void testEnabledDisabled() {
        // GIVEN a VSM
        var vsm = new TestVsm();

        // AND one enabled camera, and one disabled camera
        var enabledCam = new CameraConfiguration("Lifecam HD-3000", "Lifecam HD-3000",
                "Matt's Awesome Camera", "/dev/video0", new String[] { "/dev/v4l/by-path/foobar1" });
        var disabledCam = new CameraConfiguration("Lifecam HD-3000", "Lifecam HD-3000 (1)",
                "Another Awesome Camera", "/dev/video1", new String[] { "/dev/v4l/by-path/foobar2" });
        disabledCam.deactivated = true;

        vsm.testCameras = List.of(
                PVCameraDevice.fromUsbCameraInfo(new UsbCameraInfo(
                        0, enabledCam.path, enabledCam.baseName, enabledCam.otherPaths, enabledCam.usbVID,
                        enabledCam.usbPID)),
                PVCameraDevice.fromUsbCameraInfo(new UsbCameraInfo(
                        1, disabledCam.path, disabledCam.baseName, disabledCam.otherPaths, disabledCam.usbVID,
                        disabledCam.usbPID)));

        // WHEN cameras are loaded from disk
        vsm.registerLoadedConfigs(List.of(enabledCam, disabledCam));

        vsm.discoverNewDevices();

        // the enabled and disabled cameras will be matched
        assertEquals(1, vsm.getVsmState().activeCameras.size());
        assertEquals(1, vsm.getVsmState().disabledCameras.size());
        assertEquals(2, vsm.getVsmState().allConnectedCameras.size());

        vsm.teardown();
    }

}
