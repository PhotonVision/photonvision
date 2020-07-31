package org.photonvision.vision.frame.provider;

import org.photonvision.raspi.PicamJNI;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.pipe.impl.GPUAccelerator;
import org.photonvision.vision.processes.VisionSourceSettables;

public class AcceleratedPicamFrameProvider implements FrameProvider {

  private final VisionSourceSettables settables;
  private final GPUAccelerator accelerator;

  public AcceleratedPicamFrameProvider(VisionSourceSettables visionSettables) {
    this.settables = visionSettables;
    this.accelerator = new GPUAccelerator(GPUAccelerator.TransferMode.DIRECT_OMX);

//    boolean err = PicamJNI.createImageKHR(accelerator.getInputTextureID());
//    if (err) {
//      System.out.println("uh oh");
//      return;
//    } else {
//      System.out.println("guuci");
//    }
    PicamJNI.createCamera(1920, 1080, 60);
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
