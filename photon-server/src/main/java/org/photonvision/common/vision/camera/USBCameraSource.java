package org.photonvision.common.vision.camera;

import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.vision.frame.FrameProvider;
import org.photonvision.common.vision.frame.provider.USBFrameProvider;
import org.photonvision.common.vision.processes.VisionSource;
import org.photonvision.common.vision.processes.VisionSourceSettables;
import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.VideoMode;
import edu.wpi.first.cameraserver.CameraServer;

import java.util.*;

public class USBCameraSource implements VisionSource {
    private final UsbCamera camera;
    private final USBCameraSettables usbCameraSettables;
    private final USBFrameProvider usbFrameProvider;
    private final CameraConfiguration configuration;

    private final QuirkyCamera cameraQuirks;

    public USBCameraSource(CameraConfiguration config) {
        this.configuration = config;
        this.camera = new UsbCamera(config.nickname, config.path);
        this.cameraQuirks =
                new QuirkyCamera(camera.getInfo().productId, camera.getInfo().vendorId, config.baseName);
        CvSink cvSink = CameraServer.getInstance().getVideo(this.camera);
        this.usbCameraSettables = new USBCameraSettables(config);
        this.usbFrameProvider =
                new USBFrameProvider(cvSink, usbCameraSettables.getFrameStaticProperties());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != USBCameraSource.class) {
            return false;
        }
        USBCameraSource tmp = (USBCameraSource) obj;
        boolean i = this.cameraQuirks.quirks.equals(tmp.cameraQuirks.quirks);

        boolean r = this.configuration.uniqueName.equals(tmp.configuration.uniqueName);
        boolean c = this.configuration.baseName.equals(tmp.configuration.baseName);
        boolean j = this.configuration.nickname.equals(tmp.configuration.nickname);

        boolean k = this.camera.getInfo().name.equals(tmp.camera.getInfo().name);
        boolean x = this.camera.getInfo().productId == tmp.camera.getInfo().productId;
        boolean y = this.camera.getInfo().vendorId == tmp.camera.getInfo().vendorId;
        var t = i && r && c && j && k && x && y;
        return t;
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
            return camera.getProperty("gain").get();
        }

        @Override
        public void setGain(int gain) {
            if (cameraQuirks.quirks.contains(CameraQuirks.Gain)) {
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
}
