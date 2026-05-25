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

package org.photonvision.vision.calibration;

import io.avaje.jsonb.Json;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Point3;
import org.opencv.core.Size;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.mrcal.MrCalJNI;
import org.photonvision.vision.opencv.ImageRotationMode;
import org.photonvision.vision.opencv.Releasable;
import org.wpilib.math.geometry.Pose3d;

@Json
public class CameraCalibrationCoefficients implements Releasable {
    private static final Logger logger =
            new Logger(CameraCalibrationCoefficients.class, LogGroup.Data);

    // Mirror of
    // https://github.com/dkogan/mrcal/blob/c311b0acdb29d3f6c1a5abeaf17dc6a7e2ab10d9/mrcal/cameramodel.py#L377
    // We must pass the exact optimization state vector back to mrcal when computing uncertainty, but
    // we re-estimate the camera to object pose using SolvePNP
    // So we need to keep this + the observation camera to object around. This input shall not change
    // with "calibration rotation"
    public static record OptimizationInputs(List<Pose3d> rt_cam_ref) {}

    /** The unrotated resolution of the calibration */
    public final Size resolution;

    public final JsonMatOfDouble cameraIntrinsics;

    public final JsonMatOfDouble distCoeffs;

    public final List<BoardObservation> observations;

    public final double[] calobjectWarp;

    public final Size calobjectSize;

    public final double calobjectSpacing;

    public final CameraLensModel lensmodel;

    // Solver optimization inputs, or null if not available (e.g. legacy calibrations)
    public final OptimizationInputs optimizationInputs;

    /**
     * Contains all camera calibration data for a particular resolution of a camera. Designed for use
     * with standard opencv camera calibration matrices. For details on the layout of camera
     * intrinsics/distortion matrices, see:
     * https://docs.opencv.org/4.x/d9/d0c/group__calib3d.html#ga3207604e4b1a1758aa66acb6ed5aa65d
     *
     * @param resolution The resolution this applies to. We don't assume camera binning or try
     *     rescaling calibration
     * @param cameraIntrinsics Camera intrinsics parameters matrix, in the standard opencv form.
     * @param distCoeffs Camera distortion coefficients array. Variable length depending on order of
     *     distortion model
     * @param calobjectWarp Board deformation parameters, for calibrators that can estimate that. See:
     *     https://mrcal.secretsauce.net/formulation.html#board-deformation
     * @param observations List of snapshots used to construct this calibration
     * @param calobjectSize Dimensions of the object used to calibrate, in # of squares in
     *     width/height
     * @param calobjectSpacing Spacing between adjacent squares, in meters
     */
    public CameraCalibrationCoefficients(
            Size resolution,
            JsonMatOfDouble cameraIntrinsics,
            JsonMatOfDouble distCoeffs,
            double[] calobjectWarp,
            List<BoardObservation> observations,
            Size calobjectSize,
            double calobjectSpacing,
            CameraLensModel lensmodel,
            OptimizationInputs optimizationInputs) {
        this.resolution = resolution;
        this.cameraIntrinsics = cameraIntrinsics;
        this.distCoeffs = distCoeffs;
        this.calobjectWarp = calobjectWarp;
        this.calobjectSize = calobjectSize;
        this.calobjectSpacing = calobjectSpacing;
        this.lensmodel = lensmodel;
        this.optimizationInputs = optimizationInputs;

        // Legacy migration just to make sure that observations is at worst empty and never null
        if (observations == null) {
            observations = List.of();
        }
        this.observations = observations;
    }

    public CameraCalibrationCoefficients rotateCoefficients(ImageRotationMode rotation) {
        if (rotation == ImageRotationMode.DEG_0) {
            return this;
        }
        Mat rotatedIntrinsics = getCameraIntrinsicsMat().clone();
        Mat rotatedDistCoeffs = getDistCoeffsMat().clone();
        double cx = getCameraIntrinsicsMat().get(0, 2)[0];
        double cy = getCameraIntrinsicsMat().get(1, 2)[0];
        double fx = getCameraIntrinsicsMat().get(0, 0)[0];
        double fy = getCameraIntrinsicsMat().get(1, 1)[0];

        // only adjust p1 and p2 the rest are radial distortion coefficients

        double p1 = getDistCoeffsMat().get(0, 2)[0];
        double p2 = getDistCoeffsMat().get(0, 3)[0];

        Size rotatedImageSize = null;

        // A bunch of horrifying opaque rotation black magic. See image-rotation.md for more details.
        switch (rotation) {
            case DEG_0:
                break;
            case DEG_270_CCW:
                // FX
                rotatedIntrinsics.put(0, 0, fy);
                // FY
                rotatedIntrinsics.put(1, 1, fx);

                // CX
                rotatedIntrinsics.put(0, 2, resolution.height - cy);
                // CY
                rotatedIntrinsics.put(1, 2, cx);

                // P1
                rotatedDistCoeffs.put(0, 2, p2);
                // P2
                rotatedDistCoeffs.put(0, 3, -p1);

                // The rotated image size is the same as the unrotated image size, but the width and height
                // are swapped
                rotatedImageSize = new Size(resolution.height, resolution.width);
                break;
            case DEG_180_CCW:
                // CX
                rotatedIntrinsics.put(0, 2, resolution.width - cx);
                // CY
                rotatedIntrinsics.put(1, 2, resolution.height - cy);

                // P1
                rotatedDistCoeffs.put(0, 2, -p1);
                // P2
                rotatedDistCoeffs.put(0, 3, -p2);

                // The rotated image size is the same as the unrotated image size
                rotatedImageSize = resolution;
                break;
            case DEG_90_CCW:
                // FX
                rotatedIntrinsics.put(0, 0, fy);
                // FY
                rotatedIntrinsics.put(1, 1, fx);

                // CX
                rotatedIntrinsics.put(0, 2, cy);
                // CY
                rotatedIntrinsics.put(1, 2, resolution.width - cx);

                // P1
                rotatedDistCoeffs.put(0, 2, -p2);
                // P2
                rotatedDistCoeffs.put(0, 3, p1);

                // The rotated image size is the same as the unrotated image size, but the width and height
                // are swapped
                rotatedImageSize = new Size(resolution.height, resolution.width);
                break;
        }

        JsonMatOfDouble newIntrinsics = JsonMatOfDouble.fromMat(rotatedIntrinsics);

        JsonMatOfDouble newDistCoeffs = JsonMatOfDouble.fromMat(rotatedDistCoeffs);

        rotatedIntrinsics.release();
        rotatedDistCoeffs.release();

        return new CameraCalibrationCoefficients(
                rotatedImageSize,
                newIntrinsics,
                newDistCoeffs,
                calobjectWarp,
                observations,
                calobjectSize,
                calobjectSpacing,
                lensmodel,
                optimizationInputs);
    }

    public Mat getCameraIntrinsicsMat() {
        return cameraIntrinsics.getAsMatOfDouble();
    }

    public MatOfDouble getDistCoeffsMat() {
        return distCoeffs.getAsMatOfDouble();
    }

    public double[] getIntrinsicsArr() {
        return cameraIntrinsics.data;
    }

    public double[] getDistCoeffsArr() {
        return distCoeffs.data;
    }

    public List<BoardObservation> getObservations() {
        return observations;
    }

    @Override
    public void release() {
        cameraIntrinsics.release();
        distCoeffs.release();
    }

    @Override
    public String toString() {
        return "CameraCalibrationCoefficients [resolution="
                + resolution
                + ", cameraIntrinsics="
                + cameraIntrinsics
                + ", distCoeffs="
                + distCoeffs
                + ", observationslen="
                + observations.size()
                + ", calobjectWarp="
                + Arrays.toString(calobjectWarp)
                + "]";
    }

    public UICameraCalibrationCoefficients cloneWithoutObservations() {
        return new UICameraCalibrationCoefficients(
                resolution,
                cameraIntrinsics,
                distCoeffs,
                calobjectWarp,
                observations,
                calobjectSize,
                calobjectSpacing,
                lensmodel,
                null);
    }

    /**
     * Convert from WPILib geometry types to a raw RT array
     *
     * @return array of size [numObservations * 6] where each group of 6 is (rvec[0], rvec[1],
     *     rvec[2], tvec[0], tvec[1], tvec[2]) for the
     */
    private double[] optimizationInputsRtToRef() {
        int numObs = optimizationInputs.rt_cam_ref.size();
        double[] ret = new double[numObs * 6];

        for (int i = 0; i < numObs; i++) {
            var pose = optimizationInputs.rt_cam_ref.get(i);
            var r = pose.getRotation().toVector();
            var t = pose.getTranslation().toVector();
            ret[i * 6 + 0] = r.get(0);
            ret[i * 6 + 1] = r.get(1);
            ret[i * 6 + 2] = r.get(2);
            ret[i * 6 + 3] = t.get(0);
            ret[i * 6 + 4] = t.get(1);
            ret[i * 6 + 5] = t.get(2);
        }

        return ret;
    }

    /**
     * Estimate uncertainty across a grid of points. Returned list is (u, v, uncertainty) in pixels.
     * Please find a better home for this code
     */
    public List<Point3> estimateUncertainty() {
        if (this.optimizationInputs == null) {
            logger.error("Cannot compute uncertainty without optimization inputs");
            throw new RuntimeException("Cannot compute uncertainty without optimization inputs");
        }

        // number of intersections
        int boardWidth = (int) calobjectSize.width;
        int boardHeight = (int) calobjectSize.height;

        double[] xylevels = new double[boardWidth * boardHeight * 3 * observations.size()];
        var rt_ref_frames = optimizationInputsRtToRef();

        int xylevelsIdx = 0;
        for (var board : observations) {
            if (board.locationInImageSpace.size() != board.cornersUsed.length) {
                throw new RuntimeException("Length mismatch");
            }

            var corners = board.locationInImageSpace;

            // xylevels is row-major per chessboard
            for (int boardCornerIdx = 0; boardCornerIdx < corners.size(); boardCornerIdx++) {
                var corner = corners.get(boardCornerIdx);
                double level = board.cornersUsed[boardCornerIdx] ? 1.0 : -1.0;

                xylevels[xylevelsIdx * 3 + 0] = corner.x;
                xylevels[xylevelsIdx * 3 + 1] = corner.y;
                xylevels[xylevelsIdx * 3 + 2] = level;

                xylevelsIdx += 1;
            }
        }

        double warpX, warpY;
        if (calobjectWarp == null || calobjectWarp.length != 2) {
            warpX = 0;
            warpY = 0;
        } else {
            warpX = calobjectWarp[0];
            warpY = calobjectWarp[1];
        }

        var mrcalIntrinsics = new double[12];
        Arrays.fill(mrcalIntrinsics, 0);
        // core is fx fy cx cy
        var core = this.cameraIntrinsics.getAsWpilibMat();
        mrcalIntrinsics[0] = core.get(0, 0);
        mrcalIntrinsics[1] = core.get(1, 1);
        mrcalIntrinsics[2] = core.get(0, 2);
        mrcalIntrinsics[3] = core.get(1, 2);
        // distortion
        System.arraycopy(
                this.getDistCoeffsArr(), 0, mrcalIntrinsics, 4, this.getDistCoeffsArr().length);

        var uncertainty = // x, y, uncertainty
                MrCalJNI.compute_uncertainty(
                        xylevels,
                        mrcalIntrinsics,
                        rt_ref_frames,
                        boardWidth,
                        boardHeight,
                        calobjectSpacing,
                        (int) resolution.width,
                        (int) resolution.height,
                        60,
                        40,
                        warpX,
                        warpY);
        if (uncertainty == null) {
            logger.error("Failed to compute uncertainty");
            throw new RuntimeException("Failed to compute uncertainty");
        }

        var ret = new ArrayList<Point3>();
        for (int j = 0; j < uncertainty.length; j += 3) {
            ret.add(new Point3(uncertainty[j + 0], uncertainty[j + 1], uncertainty[j + 2]));
        }

        return ret;
    }
}
