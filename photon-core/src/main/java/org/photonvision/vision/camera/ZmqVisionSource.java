package org.photonvision.vision.camera;

import edu.wpi.first.cscore.VideoMode;
import edu.wpi.first.cscore.VideoMode.PixelFormat;
import java.util.HashMap;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.frame.provider.ZmqFrameProvider;
import org.photonvision.vision.processes.VisionSource;
import org.photonvision.vision.processes.VisionSourceSettables;

public class ZmqVisionSource extends VisionSource {
    private final ZmqFrameProvider frameProvider;
    private final ZmqSourceSettables settables;

    public ZmqVisionSource(CameraConfiguration cameraConfiguration) throws IllegalArgumentException{
        super(cameraConfiguration);
        var calibration =
            cameraConfiguration.calibrations.size() > 0
                ? cameraConfiguration.calibrations.get(0)
                : null;
        var lastSlashIndex = cameraConfiguration.path.lastIndexOf('/');
        if (lastSlashIndex == -1) {
            throw new IllegalArgumentException(
                "ZMQ path is malformatted. Should be 'tcp://{address}/{topic}' but received'" + cameraConfiguration.path + "'"
            );
        }

        var address = cameraConfiguration.path.substring(0, lastSlashIndex);
        var topic = cameraConfiguration.path.substring(lastSlashIndex + 1);
        this.frameProvider =
            new ZmqFrameProvider(
                address,
                topic,
                cameraConfiguration.FOV,
                ZmqFrameProvider.MAX_FPS,
                calibration);
                this.settables = new ZmqSourceSettables(cameraConfiguration, frameProvider.get().frameStaticProperties);
    }

    @Override
    public FrameProvider getFrameProvider() {
        return this.frameProvider;
    }

    @Override
    public VisionSourceSettables getSettables() {
        return this.settables;
    }

    @Override
    public boolean isVendorCamera() {
        return false;
    }

    private static class ZmqSourceSettables extends VisionSourceSettables {
        private final VideoMode videoMode;

        ZmqSourceSettables(CameraConfiguration cameraConfiguration, FrameStaticProperties frameStaticProperties) {
            super(cameraConfiguration);
            this.videoMode =
                    new VideoMode(
                            PixelFormat.kBGR,
                            frameStaticProperties.imageWidth,
                            frameStaticProperties.imageHeight,
                            ZmqFrameProvider.MAX_FPS);

            this.videoModes = new HashMap<>();
            this.videoModes.put(0, this.videoMode);
        }

        @Override
        public void setExposure(double exposure) {}

        public void setAutoExposure(boolean cameraAutoExposure) {}

        @Override
        public void setBrightness(int brightness) {}

        @Override
        public void setGain(int gain) {}

        @Override
        public VideoMode getCurrentVideoMode() {
            return this.videoMode;
        }

        @Override
        protected void setVideoModeInternal(VideoMode videoMode) {}

        @Override
        public HashMap<Integer, VideoMode> getAllVideoModes() {
            return this.videoModes;
        }
    }
}
