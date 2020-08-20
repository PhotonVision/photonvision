/*
 * Copyright (C) 2020 Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.photonvision.vision.pipe.impl;

import org.photonvision.vision.pipe.CVPipe;
import org.opencv.core.Mat;

public class GPUAcceleratedHSVPipe extends CVPipe<Mat, Mat, HSVPipe.HSVParams> {

  private final GPUAccelerator accelerator;

  public GPUAcceleratedHSVPipe(GPUAccelerator.TransferMode pboMode) {
    if (pboMode == GPUAccelerator.TransferMode.ZERO_COPY_OMX) {
      throw new IllegalArgumentException("The ZERO_COPY_OMX transfer mode is only supported with the GPUAcceleratedFrameProvider");
    }

    // 1280x720 is a reasonable starting resolution... It might be changed later if the user switches resolutions.
    accelerator = new GPUAccelerator(pboMode, 1280, 720);
  }

  @Override
  public Mat process(Mat in) {
    return accelerator.process(in, params.getHsvLower(), params.getHsvUpper());
  }
}
