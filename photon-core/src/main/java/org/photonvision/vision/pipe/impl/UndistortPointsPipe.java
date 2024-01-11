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

import java.util.List;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2d;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.estimation.OpenCVHelp;
import org.photonvision.mrcal.MrCalJNI;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.pipe.CVPipe;
import org.photonvision.vision.pipe.MutatingPipe;

/** Represents a pipeline that blurs the image. */
public class UndistortPointsPipe extends CVPipe<List<Point>, List<Point>, UndistortPointsPipe.UndistortParams> {
    private static final Logger logger = new Logger(UndistortPointsPipe.class, LogGroup.Vision);

    public static class UndistortParams {

        final CameraCalibrationCoefficients cal;
        final boolean useMrcal;

        public UndistortParams(CameraCalibrationCoefficients calibration, boolean useMrcal) {
            this.cal = calibration;
            this.useMrcal = useMrcal;
        }
    }

    @Override
    protected List<Point> process(List<Point> in) {
        if (params.useMrcal) {
            var src = new MatOfPoint2d(in.toArray(new Point[0]));

            var dst = new MatOfPoint2d();
            dst.alloc(src.rows());
            
            var ret = MrCalJNI.undistort_mrcal(src.nativeObj, dst.nativeObj, params.cal.cameraIntrinsics.getAsMatOfDouble().nativeObj, params.cal.distCoeffs.getAsMatOfDouble().nativeObj, 0, -1, -1, -1, -1);
            
            if (!ret) {
                logger.error("Could not undistort with mrcal!");
            }
            
            return dst.toList();
        } else {
            var distMat = new MatOfPoint2f(in.toArray(new Point[0]));
            var undistMat = new MatOfPoint2f();
            // Arbitrary precision for undistort internal iteration
            double EPSILON = 1e-6;
            Calib3d.undistortImagePoints(distMat, undistMat, params.cal.cameraIntrinsics.getAsMatOfDouble(), params.cal.distCoeffs.getAsMatOfDouble(),
                new TermCriteria(TermCriteria.COUNT + TermCriteria.EPS, 30, EPSILON));
            var ret = undistMat.toList();
            distMat.release();
            undistMat.release();
            return ret;
        }
    }
}
