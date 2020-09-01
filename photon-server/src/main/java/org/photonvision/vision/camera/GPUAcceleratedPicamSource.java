package org.photonvision.vision.camera;

import edu.wpi.cscore.VideoMode;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.raspi.PicamJNI;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.provider.AcceleratedPicamFrameProvider;
import org.photonvision.vision.processes.VisionSource;
import org.photonvision.vision.processes.VisionSourceSettables;

import java.util.HashMap;

public class GPUAcceleratedPicamSource implements VisionSource {

  private final VisionSourceSettables settables;
  private final AcceleratedPicamFrameProvider frameProvider;

  public GPUAcceleratedPicamSource(CameraConfiguration configuration) {
    if (configuration.cameraType != CameraType.ZeroCopyPicam) {
      throw new IllegalArgumentException("GPUAcceleratedPicamSource only accepts CameraConfigurations with type Picam");
    }

    settables = new PicamSettables(configuration);

    var mode = settables.getCurrentVideoMode();
    frameProvider = new AcceleratedPicamFrameProvider(settables, mode.width, mode.height);
  }

  @Override
  public FrameProvider getFrameProvider() {
    return frameProvider;
  }

  @Override
  public VisionSourceSettables getSettables() {
    return settables;
  }

  public static class PicamSettables extends VisionSourceSettables {

    private VideoMode currentVideoMode;

    public PicamSettables(CameraConfiguration configuration) {
      super(configuration);

      videoModes = new HashMap<>();
      videoModes.put(0, new VideoMode(VideoMode.PixelFormat.kUnknown, 320, 240, 100));
      videoModes.put(1, new VideoMode(VideoMode.PixelFormat.kUnknown, 640, 480, 65));
      videoModes.put(2, new VideoMode(VideoMode.PixelFormat.kUnknown, 960, 720, 55));
      videoModes.put(3, new VideoMode(VideoMode.PixelFormat.kUnknown, 1280, 720, 40));
      videoModes.put(4, new VideoMode(VideoMode.PixelFormat.kUnknown, 1920, 1080, 20));

      currentVideoMode = videoModes.get(0);
    }

    @Override
    public void setExposure(int exposure) {

    }

    @Override
    public void setBrightness(int brightness) {

    }

    @Override
    public void setGain(int gain) {

    }

    @Override
    public VideoMode getCurrentVideoMode() {
      return currentVideoMode;
    }

    @Override
    protected void setVideoModeInternal(VideoMode videoMode) {
      PicamJNI.destroyCamera();
      PicamJNI.createCamera(videoMode.width, videoMode.height, videoMode.fps);

      currentVideoMode = videoMode;
    }

    @Override
    public HashMap<Integer, VideoMode> getAllVideoModes() {
      return videoModes;
    }
  }
}
