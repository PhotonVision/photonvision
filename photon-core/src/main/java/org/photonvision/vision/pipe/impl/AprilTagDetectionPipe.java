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
import edu.wpi.first.apriltag.AprilTagDetector;
import java.util.List;
import org.opencv.core.Mat;
import org.photonvision.jni.GpuDetectorJNI;
import org.photonvision.vision.apriltag.AprilTagFamily;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.Releasable;
import org.photonvision.vision.pipe.CVPipe;

public class AprilTagDetectionPipe
        extends CVPipe<
                CVMat, List<AprilTagDetection>, AprilTagDetectionPipe.AprilTagDetectionPipeParams>
        implements Releasable {
    private AprilTagDetector m_detector = null;
    private long cudaDetector = 0;
    private boolean cudaAccelerated;

    public AprilTagDetectionPipe(boolean cudaAccelerated) {
        super();

        this.cudaAccelerated = cudaAccelerated;

        if (cudaAccelerated) {
            cudaDetector = GpuDetectorJNI.createGpuDetector(640, 480); // just a guess
        } else {
            m_detector = new AprilTagDetector();
            m_detector.addFamily("tag16h5");
            m_detector.addFamily("tag36h11");
        }
    }

    @Override
    protected List<AprilTagDetection> process(CVMat in) {
        if (in.getMat().empty()) {
            return List.of();
        }

        AprilTagDetection[] ret;
        if (cudaAccelerated) {
            if (cudaDetector == 0) {
                throw new RuntimeException("CUDA Apriltag detector was released!");
            }
            ret = GpuDetectorJNI.processimage(cudaDetector, in.getMat().getNativeObjAddr());
        } else {
            if (m_detector == null) {
                throw new RuntimeException("Apriltag detector was released!");
            }
            ret = m_detector.detect(in.getMat());
        }

        if (ret == null) {
            return List.of();
        }

        return List.of(ret);
    }

    @Override
    public void setParams(AprilTagDetectionPipeParams newParams) {
        if (this.params == null || !this.params.equals(newParams)) {
            if (this.cudaAccelerated != newParams.useCuda) {
                if (newParams.useCuda) {
                    cudaDetector = GpuDetectorJNI.createGpuDetector(640, 480);
                    m_detector.close();
                    m_detector = null;
                } else {
                    m_detector = new AprilTagDetector();
                    m_detector.addFamily("tag16h5");
                    m_detector.addFamily("tag36h11");
                    GpuDetectorJNI.destroyGpuDetector(cudaDetector);
                }
                this.cudaAccelerated = newParams.useCuda;
            }
            if (cudaAccelerated) {
                if (newParams.cal == null) return;

                final Mat cameraMatrix = newParams.cal.getCameraIntrinsicsMat();
                final Mat distCoeffs = newParams.cal.getDistCoeffsMat();
                if (cameraMatrix == null || distCoeffs == null) return;
                var cx = cameraMatrix.get(0, 2)[0];
                var cy = cameraMatrix.get(1, 2)[0];
                var fx = cameraMatrix.get(0, 0)[0];
                var fy = cameraMatrix.get(1, 1)[0];
                var k1 = distCoeffs.get(0, 0)[0];
                var k2 = distCoeffs.get(0, 1)[0];
                var k3 = distCoeffs.get(0, 4)[0];
                var p1 = distCoeffs.get(0, 2)[0];
                var p2 = distCoeffs.get(0, 3)[0];

                GpuDetectorJNI.setparams(cudaDetector, fx, cx, fy, cy, k1, k2, p1, p2, k3);
            } else {
                m_detector.setConfig(newParams.detectorParams());
                m_detector.setQuadThresholdParameters(newParams.quadParams());

                m_detector.clearFamilies();
                m_detector.addFamily(newParams.family().getNativeName());
            }
        }

        super.setParams(newParams);
    }

    @Override
    public void release() {
        if (cudaAccelerated) {
            GpuDetectorJNI.destroyGpuDetector(cudaDetector);
            cudaDetector = 0;
        } else {
            m_detector.close();
            m_detector = null;
        }
    }

    public static record AprilTagDetectionPipeParams(
            AprilTagFamily family,
            AprilTagDetector.Config detectorParams,
            AprilTagDetector.QuadThresholdParameters quadParams,
            CameraCalibrationCoefficients cal,
            boolean useCuda) {}
}
