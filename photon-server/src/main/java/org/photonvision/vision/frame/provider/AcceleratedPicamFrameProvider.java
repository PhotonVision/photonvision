package org.photonvision.vision.frame.provider;

import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.processes.VisionSourceSettables;

public class AcceleratedPicamFrameProvider implements FrameProvider {

  private final VisionSourceSettables settables;

  public AcceleratedPicamFrameProvider(VisionSourceSettables visionSettables) {
    this.settables = visionSettables;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public Frame get() {
    return null;
  }
}
