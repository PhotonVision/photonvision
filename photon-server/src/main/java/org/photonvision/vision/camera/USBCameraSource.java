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

package org.photonvision.vision.camera;

import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.VideoMode;
import edu.wpi.cscore.VideoSource;
import edu.wpi.first.cameraserver.CameraServer;
import java.util.*;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.frame.provider.USBFrameProvider;
import org.photonvision.vision.processes.VisionSource;
import org.photonvision.vision.processes.VisionSourceSettables;

public class USBCameraSource implements VisionSource {
    private final UsbCamera camera;
    private final USBCameraSettables usbCameraSettables;
    private final USBFrameProvider usbFrameProvider;
    public final CameraConfiguration configuration;
    private final CvSink cvSink;

    private final QuirkyCamera cameraQuirks;

    public USBCameraSource(CameraConfiguration config) {
        configuration = config;
        camera = new UsbCamera(config.nickname, config.path);
        cameraQuirks = QuirkyCamera.getQuirkyCamera(camera.getInfo().productId, camera.getInfo().vendorId, config.baseName);
        cvSink = CameraServer.getInstance().getVideo(this.camera);
        usbCameraSettables = new USBCameraSettables(config);
        usbFrameProvider =
                new USBFrameProvider(cvSink, usbCameraSettables.getFrameStaticProperties());
    }

    @Override
    public FrameProvider getFrameProvider() {
        return usbFrameProvider;
    }

    @Override
    public VisionSourceSettables getSettables() {
        return this.usbCameraSettables;
    }

    public class USBCameraSettables extends VisionSourceSettables {
        protected USBCameraSettables(CameraConfiguration configuration) {
            super(configuration);
            getAllVideoModes();
            setCurrentVideoMode(videoModes.get(0));
            frameStaticProperties = new FrameStaticProperties(getCurrentVideoMode(), getFOV());
        }

        @Override
        public int getExposure() {
            return camera.getProperty("exposure").get();
        }

        @Override
        public void setExposure(int exposure) {
            camera.setExposureManual(exposure);
        }

        @Override
        public int getBrightness() {
            return camera.getBrightness();
        }

        @Override
        public void setBrightness(int brightness) {
            camera.setBrightness(brightness);
        }

        @Override
        public int getGain() {
            return !cameraQuirks.hasQuirk(CameraQuirk.Gain) ? -1 : camera.getProperty("gain").get();
        }

        @Override
        public void setGain(int gain) {
            if (cameraQuirks.hasQuirk(CameraQuirk.Gain)) {
                camera.getProperty("gain_automatic").set(0);
                camera.getProperty("gain").set(gain);
            }
        }

        @Override
        public VideoMode getCurrentVideoMode() {
            return camera.getVideoMode();
        }

        @Override
        public void setCurrentVideoMode(VideoMode videoMode) {
            camera.setVideoMode(videoMode);
        }

        @Override
        public HashMap<Integer, VideoMode> getAllVideoModes() {
            if (videoModes == null) {
                videoModes = new HashMap<>();
                List<VideoMode> videoModesList = Arrays.asList(camera.enumerateVideoModes());
                for (VideoMode videoMode : videoModesList) {
                    videoModes.put(videoModesList.indexOf(videoMode), videoMode);
                }
            }
            return videoModes;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        USBCameraSource that = (USBCameraSource) o;
        return Objects.equals(camera, that.camera) &&
                Objects.equals(usbFrameProvider, that.usbFrameProvider) &&
                Objects.equals(configuration, that.configuration) &&
                cameraQuirks.equals(that.cameraQuirks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(camera, usbCameraSettables, usbFrameProvider, configuration, cvSink, cameraQuirks);
    }
}
