package org.photonvision.vision.camera;

import edu.wpi.cscore.VideoMode;
import org.photonvision.common.configuration.CameraConfiguration;
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

    private final HashMap<Integer, VideoMode> videoModes = new HashMap<>();

    public PicamSettables(CameraConfiguration configuration) {
      super(configuration);

      videoModes.put(0, new VideoMode(VideoMode.PixelFormat.kUnknown, 960, 720, 90));
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
      return videoModes.get(0);
    }

    @Override
    protected void setVideoModeInternal(VideoMode videoMode) {

    }

    @Override
    public HashMap<Integer, VideoMode> getAllVideoModes() {
      return videoModes;
    }
  }
}
