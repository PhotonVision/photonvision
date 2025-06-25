/*
 * Copyright (C) Photon Vision.
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

import edu.wpi.first.apriltag.AprilTagDetection;
import java.util.List;
import org.opencv.core.Mat;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.jni.GpuDetectorJNI;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.Releasable;
import org.photonvision.vision.pipe.CVPipe;

public class AprilTagDetectionCudaPipe
    extends CVPipe<CVMat, List<AprilTagDetection>, AprilTagDetectionCudaPipeParams>
    implements Releasable {
  private GpuDetectorJNI m_cudadetector = new GpuDetectorJNI();
  private long handle = 0;

  public AprilTagDetectionCudaPipe() {
    super();

    // handle = m_cudadetector.createGpuDetector(640,480); // just a guess Thats
    // crazy (Charlie)
    handle = m_cudadetector.createGpuDetector(1456, 1088); // just a guess
  }

  @Override
  protected List<AprilTagDetection> process(CVMat in) {
    var start = MathUtils.wpiNanoTime();

    if (in.getMat().empty()) {
      System.out.println("empty");
      return List.of();
    }

    var ret = m_cudadetector.processimage(handle, in.getMat().getNativeObjAddr());

    var end = MathUtils.wpiNanoTime();
    System.out.println("AprilTag Cuda Latency: " + (end - start) / 1000_000);

    if (ret == null) {
      return List.of();
    }

    return List.of(ret);
  }

  @Override
  public void setParams(AprilTagDetectionCudaPipeParams newParams) {
    if (this.params == null || !this.params.equals(newParams)) {
      if (newParams.cameraCalibrationCoefficients == null)
        return;

      final Mat cameraMatrix = newParams.cameraCalibrationCoefficients.getCameraIntrinsicsMat();
      final Mat distCoeffs = newParams.cameraCalibrationCoefficients.getDistCoeffsMat();
      if (cameraMatrix == null || distCoeffs == null)
        return;
      var cx = cameraMatrix.get(0, 2)[0];
      var cy = cameraMatrix.get(1, 2)[0];
      var fx = cameraMatrix.get(0, 0)[0];
      var fy = cameraMatrix.get(1, 1)[0];
      var k1 = distCoeffs.get(0, 0)[0];
      var k2 = distCoeffs.get(0, 1)[0];
      var k3 = distCoeffs.get(0, 4)[0];
      var p1 = distCoeffs.get(0, 2)[0];
      var p2 = distCoeffs.get(0, 3)[0];

      m_cudadetector.setparams(handle, fx, cx, fy, cy, k1, k2, p1, p2, k3);
    }

    super.setParams(newParams);
  }

  @Override
  public void release() {
    m_cudadetector.destroyGpuDetector(handle);
    m_cudadetector = null;
  }
}
