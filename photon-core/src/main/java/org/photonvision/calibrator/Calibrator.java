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

// This project and file are derived in part from the "Pose Calib" project by
// @author Pavel Rojtberg
// It is subject to his license terms in the PoseCalibLICENSE file.

package org.photonvision.calibrator;

import java.util.ArrayList;
import java.util.List;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
/*                                                                                                 */
/*                                     Calibrator class                                            */
/*                                     Calibrator class                                            */
/*                                     Calibrator class                                            */
/*                                                                                                 */
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
class Calibrator {
    private static final Logger logger = new Logger(Calibrator.class, LogGroup.General);
    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    /*                                                                                                 */
    /*                                     Calibrator constructor                                      */
    /*                                     Calibrator constructor                                      */
    /*                                     Calibrator constructor                                      */
    /*                                                                                                 */
    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    private Size img_size;
    private int nintr = 9; // "fx", "fy", "cx", "cy", "k1", "k2", "p1", "p2", "k3"
    // unknowns not used
    private Mat Kin;
    private Mat K = new Mat();
    private Mat cdist = Mat.zeros(1, 5, CvType.CV_64FC1);
    private int flags = Calib3d.CALIB_USE_LU;

    // calibration data
    List<keyframe> keyframes = new ArrayList<>(20);
    // private int N_pts = 0; // not used - only for Jacobian covariance that was removed
    private double reperr = Double.NaN;
    private double[] varIntrinsics = {0., 0., 0., 0., 0., 0., 0., 0., 0.}; // nintr length
    private double[] pose_var = {0., 0., 0., 0., 0., 0.}; // rotation x, y, z and translation x, y, z
    private double[]
            disp_idx = // index of dispersion. One for each intrinsic this program handles (nintr=9)
            {
        Double.NaN,
        Double.NaN,
        Double.NaN,
        Double.NaN,
        Double.NaN,
        Double.NaN,
        Double.NaN,
        Double.NaN,
        Double.NaN
    };

    // getters
    Size img_size() {
        return this.img_size;
    }

    int nintr() {
        return this.nintr;
    }

    double reperr() {
        return this.reperr;
    }

    double[] varIntrinsics() {
        return this.varIntrinsics;
    }

    double[] pose_var() {
        return this.pose_var;
    }

    int flags() {
        return this.flags;
    }

    Mat K() {
        return this.K;
    }

    Mat cdist() {
        return this.cdist;
    }

    Calibrator(Size img_size) {
        logger.debug("Starting ----------------------------------------");
        this.img_size = img_size;
        // initial fake camera matrix to get things started
        // initial K matrix
        // with aspect ratio of 1 and pp at center. Focal length is empirical.
        this.Kin = Mat.zeros(3, 3, CvType.CV_64FC1);
        this.Kin.put(0, 0, Cfg.initialFocalLength);
        this.Kin.put(1, 1, Cfg.initialFocalLength);
        this.Kin.put(2, 2, 1.);
        this.Kin = Calib3d.getDefaultNewCameraMatrix(this.Kin, img_size, true);
        this.Kin.copyTo(this.K);
        // logger.debug("K/Kin\n" + this.K.dump() + "\n" + this.Kin.dump());
    }

    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    /*                                                                                                 */
    /*                                     get_intrinsics                                              */
    /*                                     get_intrinsics                                              */
    /*                                     get_intrinsics                                              */
    /*                                                                                                 */
    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    private double[] get_intrinsics() {
        // logger.debug("method entered  . . . . . . . . . . . . . . . . . . . . . . . .");
        double[] intrinsics = {
            this.K.get(0, 0)[0],
            this.K.get(1, 1)[0],
            this.K.get(0, 2)[0],
            this.K.get(1, 2)[0],
            this.cdist.get(0, 0)[0],
            this.cdist.get(0, 1)[0],
            this.cdist.get(0, 2)[0],
            this.cdist.get(0, 3)[0],
            this.cdist.get(0, 4)[0]
        };
        // logger.debug("K\n" + K.dump());
        // logger.debug(java.util.Arrays.toString(intrinsics));
        return intrinsics;
    }

    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    /*                                                                                                 */
    /*                                     calibrate                                                   */
    /*                                     calibrate                                                   */
    /*                                     calibrate                                                   */
    /*                                                                                                 */
    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    double[] calibrate(List<keyframe> keyframes)
            throws Exception // force use of keyframes instead of default None
            {
        // logger.debug("method entered  . . . . . . . . . . . . . . . . . . . . . . . .");

        int flags = this.flags;

        if (keyframes.isEmpty()) // pose #1 hasn't been captured yet
        {
            keyframes.addAll(
                    this.keyframes); // gives size 1 keyframe for the first 2 poses so hard to tell the 2nd
            // pose is in action since it's still #1
        }

        if (keyframes.isEmpty()) {
            throw new Exception("keyframes is empty");
        }

        int nkeyframes = keyframes.size();

        // initialization process
        if (nkeyframes <= 1)
        // first pose hasn't been captured yet so it's 0 + the initial keyframe from above making 1
        // second pose has the previous captured pose #1 and the second pose hasn't been captured yet
        // making it #1, also.
        // thus, RKT saved the pose that was last used.
        // Probably could have determined what to do by saving the number of keyframes on entry to this
        // method the first is 0 and the second is 1.

        // The flags were modified from the original under the assumption that this is what the author
        // meant to do but could not get it to
        // work right because of a (likely) bug that trashed the initial K values. Hope the two fixes
        // are right - seems to work better this way.
        {
            switch (PoseGeneratorDist.pose) {
                case ORBITAL:
                    // restrict early orbital calibrations to K matrix parameters and don't mess with
                    // distortion
                    flags |= Calib3d.CALIB_FIX_ASPECT_RATIO;
                    flags |=
                            Calib3d.CALIB_ZERO_TANGENT_DIST
                                    | Calib3d.CALIB_FIX_K1
                                    | Calib3d.CALIB_FIX_K2
                                    | Calib3d.CALIB_FIX_K3;
                    break;

                case PLANAR_FULL_SCREEN:
                    // restrict early planar calibrations to distortion and don't mess with focal length
                    flags |= Calib3d.CALIB_FIX_PRINCIPAL_POINT | Calib3d.CALIB_FIX_FOCAL_LENGTH;
                    break;

                default:
                    logger.error("unknown initial pose " + PoseGeneratorDist.pose);
                    break;
            }
        }

        calibrateCameraReturn res = calibrateCamera(keyframes, this.img_size, flags, this.Kin);

        this.reperr = res.reperr;
        res.K.copyTo(this.K);
        res.cdist.copyTo(this.cdist);
        List<Mat> rvecsList = res.rvecsList;
        List<Mat> tvecsList = res.tvecsList;
        Mat variances = res.varianceIntrinsics;

        variances.get(
                0, 0, this.varIntrinsics); // convert from Mat to double array; this.varIntrinsics replaces
        // self.PCov in Python

        this.pose_var = compute_pose_var(rvecsList, tvecsList);

        this.disp_idx = index_of_dispersion(this.get_intrinsics(), this.varIntrinsics);

        return this.disp_idx;
    }

    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    /*                                                                                                 */
    /*                                     index_of_dispersion                                         */
    /*                                     index_of_dispersion                                         */
    /*                                     index_of_dispersion                                         */
    /*                                                                                                 */
    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    private static double[] index_of_dispersion(double[] mean, double[] variance) {
        // logger.debug("method entered  . . . . . . . . . . . . . . . . . . . . . . . .");

        // computes index of dispersion:
        // https://en.wikipedia.org/wiki/Index_of_dispersion
        // compute the 9 VMR's
        if (mean.length != variance.length) {
            logger.error("mean and variance not the same size");
        }
        double[] VMR = new double[9];
        for (int i = 0; i < mean.length; i++) {
            VMR[i] = variance[i] / (Math.abs(mean[i]) > 0 ? Math.abs(mean[i]) : 1.);
        }

        return VMR;
    }

    /**
     * mean_extr_var and estimate_pt_std NOT USED -- NOT CONVERTED mean_extr_var and estimate_pt_std
     * NOT USED -- NOT CONVERTED mean_extr_var and estimate_pt_std NOT USED -- NOT CONVERTED
     */

    /**
     * Jc2J and compute_state_cov replaced by OpenCV std dev -- NOT CONVERTED Jc2J and
     * compute_state_cov replaced by OpenCV std dev -- NOT CONVERTED Jc2J and compute_state_cov
     * replaced by OpenCV std dev -- NOT CONVERTED
     */

    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    /*                                                                                                 */
    /*                                     compute_pose_var                                            */
    /*                                     compute_pose_var                                            */
    /*                                     compute_pose_var                                            */
    /*                                                                                                 */
    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    /**
     * Compute variances and prints the euler angles in degrees for debugging
     *
     * @param rvecs data 1 - List of triplets
     * @param tvecs data 2 - List of triplets
     * @return variances of data 1 and data 2 - a sextet
     */
    private static double[] compute_pose_var(List<Mat> rvecs, List<Mat> tvecs) {
        // logger.debug("method entered  . . . . . . . . . . . . . . . . . . . . . . . .");

        double[] ret = new double[6]; // return

        List<double[]> reulers = new ArrayList<>(50);

        // temporary Mats for inside loop but create them only once for efficiency
        Mat dst = new Mat();
        Mat mtxR = new Mat();
        Mat mtxQ = new Mat();

        for (Mat r : rvecs) {
            Calib3d.Rodrigues(r, dst);
            double[] reuler = Calib3d.RQDecomp3x3(dst, mtxR, mtxQ); // always returns reuler.length = 3
            // logger.debug("\nreuler degrees " + java.util.Arrays.toString(reuler) + "\nr " +
            // r.t().dump());
            // workaround for the given board so r_x does not oscilate between +-180°
            reuler[0] = reuler[0] % 360.;
            reulers.add(reuler);
        }

        double[] varReuler = simpleVariance(reulers);

        for (int i = 0; i < 3; i++) {
            ret[i] = varReuler[i];
        }

        List<double[]> translations = new ArrayList<>(50);
        for (Mat t : tvecs) {
            double[] tvec = new double[3];
            t.get(0, 0, tvec);

            for (int i = 0; i < 3; i++) {
                tvec[i] /=
                        10.; // [mm]  (not sure if this is the unity measure of arbitrary but consistent units
                // or millimeters; why divide by 10?)
            }
            translations.add(tvec);
        }

        double[] varTvecs = simpleVariance(translations);
        for (int i = 3; i < 6; i++) {
            ret[i] = varTvecs[i - 3];
        }

        dst.release();
        mtxR.release();
        mtxQ.release();

        return ret;
    }

    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    /*                                                                                                 */
    /*                                     simpleVariance                                              */
    /*                                     simpleVariance                                              */
    /*                                     simpleVariance                                              */
    /*                                                                                                 */
    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    /**
     * Variance of each axis of a list of 3-D points [Could be generalized to any number of dimensions
     * with little change.]
     *
     * @param data
     * @return variances in a 3 element array
     */
    private static double[] simpleVariance(List<double[]> data) {
        // logger.debug("method entered  . . . . . . . . . . . . . . . . . . . . . . . .");

        // always 3 components x, y, z so do all 3 at once
        double[] sum = {0., 0., 0.};
        double[] sumSqr = {0., 0., 0.};
        for (double[] datum : data) {
            for (int i = 0; i < 3; i++) {
                sum[i] += datum[i];
            }
        }

        double[] mean = {sum[0] / data.size(), sum[1] / data.size(), sum[2] / data.size()};

        for (double[] datum : data) {
            for (int i = 0; i < 3; i++) {
                sumSqr[i] += Math.pow(datum[i] - mean[i], 2);
            }
        }

        double[] variance = {sumSqr[0] / data.size(), sumSqr[1] / data.size(), sumSqr[2] / data.size()};

        return variance;
    }

    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    /*                                                                                                 */
    /*                                     calibrateCamera                                             */
    /*                                     calibrateCamera                                             */
    /*                                     calibrateCamera                                             */
    /*                                                                                                 */
    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    /**
     * @param keyframes
     * @param img_size
     * @param flags
     * @param K
     * @return
     * @throws Exception
     */
    calibrateCameraReturn calibrateCamera(List<keyframe> keyframes, Size img_size, int flags, Mat K)
            throws Exception {
        // logger.debug("method entered  . . . . . . . . . . . . . . . . . . . . . . . .");

        // split keyframes into its two separate components, image points and object points, for OpenCV
        // calibrateCamera
        // we put them together when detected then we take them apart for calibration.

        List<Mat> pts2dFrames = new ArrayList<>(40); // image points
        List<Mat> pts3dFrames = new ArrayList<>(40); // object points
        int N = 0; // count total number of points

        for (keyframe keyframe : keyframes) {
            pts2dFrames.add(keyframe.p2d());
            pts3dFrames.add(keyframe.p3d());
            N +=
                    keyframe
                            .p2d()
                            .rows(); // total points - all frames (poses/views) and all points in those poses
        }

        if (N <= 4) throw new Exception("not enough total points");

        Mat cdist = new Mat();
        List<Mat> rvecs = new ArrayList<>();
        List<Mat> tvecs = new ArrayList<>();
        Mat stdDeviationsIntrinsics = new Mat();
        double reperr = Double.NaN;
        // logger.debug("K input to calibration\n" + K.dump());
        // logger.debug(UserGuidance.formatFlags(flags));
        try {
            reperr =
                    Calib3d.calibrateCameraExtended(
                            pts3dFrames,
                            pts2dFrames,
                            img_size,
                            K,
                            cdist,
                            rvecs,
                            tvecs,
                            stdDeviationsIntrinsics,
                            new Mat(),
                            new Mat(),
                            flags,
                            Cfg.calibrateCameraCriteria);

            // logger.debug("camera matrix K " + K + "\n" + K.dump());
            // logger.debug("distortion coefficients " + cdist.dump() + cdist);
            // logger.debug("repError " + reperr);
        } catch (CvException error) {
            logger.error("Calib3d.calibrateCameraExtended error", error);
        }
        // logger.debug("K output from calibration\n" + K.dump());
        Mat varianceIntrinsics = new Mat();
        Core.multiply(
                stdDeviationsIntrinsics,
                stdDeviationsIntrinsics,
                varianceIntrinsics); // variance = stddev squared

        // logger.debug("cdist " + cdist.dump() + ", N = " + N);

        stdDeviationsIntrinsics.release();

        return new calibrateCameraReturn(reperr, K, cdist, rvecs, tvecs, varianceIntrinsics);
    }

    class calibrateCameraReturn {
        double reperr;
        Mat K;
        Mat cdist;
        List<Mat> rvecsList;
        List<Mat> tvecsList;
        Mat varianceIntrinsics;

        calibrateCameraReturn(
                double reperr, Mat K, Mat cdist, List<Mat> rvecs, List<Mat> tvecs, Mat varianceIntrinsics) {
            this.reperr = reperr;
            this.K = K;
            this.cdist = cdist;
            this.rvecsList = rvecs;
            this.tvecsList = tvecs;
            this.varianceIntrinsics = varianceIntrinsics;
            // N_pts not used
        }
    }
}
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
/*                                                                                                 */
/*                                     End Calibrator class                                        */
/*                                     End Calibrator class                                        */
/*                                     End Calibrator class                                        */
/*                                                                                                 */
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
