package org.photonvision.vision.camera.csi;

import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.camera.CameraType;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.provider.GstreamerFrameProvider;
import org.photonvision.vision.processes.VisionSource;
import org.photonvision.vision.processes.VisionSourceSettables;

public class GstreamerSource extends VisionSource {
  private final GstreamerSettables settables;
  private final GstreamerFrameProvider frameProvider;

  public GstreamerSource(CameraConfiguration configuration) {
    super(configuration);

    if (configuration.matchedCameraInfo.type() != CameraType.GstreamerCamera) {
      throw new IllegalArgumentException(
          "GstreamerSource only accepts CameraConfigurations with type GstreamerCamera");
    }

    settables = new GstreamerSettables(configuration);
    frameProvider = new GstreamerFrameProvider(settables);

    if (getCameraConfiguration().cameraQuirks == null)
        getCameraConfiguration().cameraQuirks = QuirkyCamera.ZeroCopyPiCamera;
  }

  @Override
  public FrameProvider getFrameProvider() {
    return frameProvider;
  }

  @Override
  public VisionSourceSettables getSettables() {
    return settables;
  }

  @Override
  public void remakeSettables() {}

  @Override
  public boolean hasLEDs() {
    return false;
  }

  @Override
  public void release() {
    frameProvider.release();
  }

  @Override
  public boolean isVendorCamera() {
    return false;
  }
}
