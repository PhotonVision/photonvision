package org.photonvision.vision.camera;

import edu.wpi.cscore.VideoMode;
import edu.wpi.cscore.VideoMode.PixelFormat;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.frame.provider.FileFrameProvider;
import org.photonvision.vision.processes.VisionSource;
import org.photonvision.vision.processes.VisionSourceSettables;

import java.util.HashMap;

public class FileVisionSource implements VisionSource {

    private final CameraConfiguration cameraConfiguration;
    private final FileFrameProvider frameProvider;
    private final FileSourceSettables settables;

   public FileVisionSource(String name, String imagePath, double fov) {
        cameraConfiguration = new CameraConfiguration(name, imagePath);
        frameProvider = new FileFrameProvider(imagePath, fov);
        settables = new FileSourceSettables(cameraConfiguration, frameProvider.get().frameStaticProperties);
   }

    @Override
    public FrameProvider getFrameProvider() {
        return frameProvider;
    }

    @Override
    public VisionSourceSettables getSettables() {
        return settables;
    }

    private static class FileSourceSettables extends VisionSourceSettables {

        private final VideoMode videoMode;

        private final HashMap<Integer, VideoMode> videoModes = new HashMap<>();

        FileSourceSettables(CameraConfiguration cameraConfiguration, FrameStaticProperties frameStaticProperties) {
            super(cameraConfiguration);
            this.frameStaticProperties = frameStaticProperties;
            videoMode = new VideoMode(PixelFormat.kMJPEG, frameStaticProperties.imageWidth, frameStaticProperties.imageHeight, 30);
            videoModes.put(0, videoMode);
        }

        @Override
        public void setExposure(int exposure) {}

        @Override
        public void setBrightness(int brightness) {}

        @Override
        public void setGain(int gain) {}

        @Override
        public VideoMode getCurrentVideoMode() {
            return videoMode;
        }

        @Override
        public void setCurrentVideoMode(VideoMode videoMode) {}
        @Override
        public HashMap<Integer, VideoMode> getAllVideoModes() {
            return videoModes;
        }
    }
}
