package org.photonvision.vision.pipe.impl;

import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.pipe.CVPipe;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import jogamp.opengl.GLOffscreenAutoDrawableImpl;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2ES2.*;

public class GPUAcceleratedHSVPipe extends CVPipe<Mat, Mat, HSVPipe.HSVParams> {

  private final GPUAccelerator accelerator;

  public GPUAcceleratedHSVPipe(GPUAccelerator.TransferMode pboMode) {
    if (pboMode == GPUAccelerator.TransferMode.DIRECT_OMX) {
      throw new IllegalArgumentException("The DIRECT_OMX transfer mode is only supported with the GPUAcceleratedFrameProvider");
    }

    accelerator = new GPUAccelerator(pboMode);
  }

  @Override
  protected Mat process(Mat in) {
    return accelerator.process(in, params.getHsvLower(), params.getHsvUpper());
  }
}
