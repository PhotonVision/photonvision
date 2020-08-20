package org.photonvision.vision.frame.provider;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.photonvision.raspi.PicamJNI;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipe.impl.GPUAccelerator;
import org.photonvision.vision.processes.VisionSourceSettables;

public class AcceleratedPicamFrameProvider implements FrameProvider {

  private final VisionSourceSettables settables;
  private final GPUAccelerator accelerator;
  private final CVMat mat;

  public AcceleratedPicamFrameProvider(VisionSourceSettables visionSettables, int width, int height) {
    this.settables = visionSettables;
    this.accelerator = new GPUAccelerator(GPUAccelerator.TransferMode.ZERO_COPY_OMX, width, height);
    mat = new CVMat(new Mat(height, width, CvType.CV_8UC1));
  }

  @Override
  public String getName() {
    return "AcceleratedPicamFrameProvider";
  }

  @Override
  public Frame get() {
    if (mat.getMat() != null) {
      mat.release();
    }

    // TODO: Figure out how to pass in HSV bounds
    long time = System.currentTimeMillis();
    accelerator.redrawGL(new Scalar(0, 0, 0), new Scalar(128, 128, 128));

    PicamJNI.grabFrame(mat.getMat().nativeObj);
    return new Frame(mat, time, settables.getFrameStaticProperties());
  }
}
