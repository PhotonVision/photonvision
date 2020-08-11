package org.photonvision.vision.frame.provider;

import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
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

    boolean err;

    System.out.println("starting...");

//    err = PicamJNI.createImageKHR(accelerator.getInputTextureID());
//    if (err) {
//      System.out.println("uh oh");
//      return;
//    } else {
//      System.out.println("guuci");
//    }
//    err = PicamJNI.createCamera(1280, 720, 60);
//    if (err) {
//      System.out.println("bade");
//    } else {
//      System.out.println("goodee");
//    }

    while (true) {
      var mat = accelerator.redrawGL(new Scalar(0, 0, 0), new Scalar(1, 1, 1), 960, 720);
//      Imgcodecs.imwrite("foo.png", mat);
//      try {
//        Thread.sleep(1000);
//      } catch (InterruptedException e) {
//        e.printStackTrace();
//      }
    }
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
