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

package org.photonvision.vision.pipe.impl.Calibrate3dPoseGuidance;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
/*                                                                                                 */
/*                                     PoseGeneratorDist class                                     */
/*                                     PoseGeneratorDist class                                     */
/*                                     PoseGeneratorDist class                                     */
/*                                                                                                 */
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
public class PoseGeneratorDist {
    private static final Logger logger = new Logger(PoseGeneratorDist.class, LogGroup.General);

/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
/*                                                                                                 */
/*                                     Gen_Bin class                                               */
/*                                     Gen_Bin class                                               */
/*                                     Gen_Bin class                                               */
/*                                                                                                 */
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
    // generate values in range by binary subdivision
    // note that the list of values grows exponentially with each call for a value
    // seems like a poor algorithm to me but it is used sparsely in this application
    class Gen_Bin
    {
        private List<Double> lst = new ArrayList<>(40); // always manipulate 2 items at a time instead of making a new datatype for a pair - not so safe, though
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
/*                                                                                                 */
/*                                     Gen_Bin constructor                                         */
/*                                     Gen_Bin constructor                                         */
/*                                     Gen_Bin constructor                                         */
/*                                                                                                 */
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
        /*private*/ Gen_Bin(double s, double e)
        {
            double t = (s + e) / 2.;
            this.lst.add(s);
            this.lst.add(t);
            this.lst.add(t);
            this.lst.add(e);
        }
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
/*                                                                                                 */
/*                                     next                                                        */
/*                                     next                                                        */
/*                                     next                                                        */
/*                                                                                                 */
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
        /*private*/ double next()
        {
            double s = this.lst.get(0); // pop the top
            this.lst.remove(0);

            double e = this.lst.get(0); // pop the new top
            this.lst.remove(0);

            double t = (s + e) / 2.;
            this.lst.add(s);
            this.lst.add(t);
            this.lst.add(t);
            this.lst.add(e);

            return t;
        }
    }
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
/*                                                                                                 */
/*                                     unproject                                                   */
/*                                     unproject                                                   */
/*                                     unproject                                                   */
/*                                                                                                 */
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
/**
 *     project pixel back to a 3D coordinate at depth Z
 * @param p
 * @param K
 * @param cdist
 * @param Z
 * @return
 */
    private static Mat unproject(MatOfPoint2f p, Mat K, Mat cdist, double Z)
    {
        // logger.debug("method entered  . . . . . . . . . . . . . . . . . . . . . . . .");

        // returns a translation (t) Mat
        // logger.debug("p in " + p.dump());
        // logger.debug("camera matrix K " + K + "\n" + K.dump());
        // logger.debug("cdist " + cdist.dump());
        // logger.debug("Z " + Z);

        Calib3d.undistortPointsIter(p, p, K, cdist, new Mat(), new Mat(), Cfg.undistortPointsIterCriteria);

        // logger.debug("p out " + p.dump());

        double[] pXY = p.get(0, 0); // get X and Y channels for the point (ravel)
 
        Mat p3D = new Mat(1, 3, CvType.CV_64FC1);
        p3D.put(0, 0, pXY[0], pXY[1], 1.); // x, y, 1

        Core.multiply(p3D, new Scalar(Z, Z, Z), p3D);

        // logger.debug("return p3D Z scaled " + p3D.dump());

        return p3D;
    }
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
/*                                                                                                 */
/*                                     orbital_pose                                                */
/*                                     orbital_pose                                                */
/*                                     orbital_pose                                                */
/*                                                                                                 */
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
/**
 * 
 * @param bbox object bounding box. note: assumes planar object with virtual Z dimension.
 * @param rx rotation around x axis in rad
 * @param ry rotation around y axis in rad
 * @param Z distance to camera in board lengths
 * @param rz
 * @return rvec, tvec
 */
    private static List<Mat> orbital_pose(Mat bbox, double rx, double ry, double Z, double rz) // force caller to use rz=0 if defaulting
    {
        // logger.debug("method entered  . . . . . . . . . . . . . . . . . . . . . . . .");

        // logger.debug("bbox " + bbox + "\n" + bbox.dump());
        // logger.debug("rx " + rx);
        // logger.debug("ry " + ry);
        // logger.debug("Z " + Z);
        // logger.debug("rz " + rz);
        
        // compute rotation matrix from rotation vector
        double[] angleZ = {0., 0., rz};
        double[] angleX = {Math.PI + rx, 0., 0.}; // flip by 180° so Z is up
        double[] angleY = {0., ry, 0.};
        Mat angleZVector = new Mat(3, 1, CvType.CV_64FC1);
        Mat angleXVector = new Mat(3, 1, CvType.CV_64FC1);
        Mat angleYVector = new Mat(3, 1, CvType.CV_64FC1);
        angleZVector.put(0, 0, angleZ);       
        angleXVector.put(0, 0, angleX);
        angleYVector.put(0, 0, angleY);
        Mat Rz = new Mat();
        Mat Rx = new Mat();
        Mat Ry = new Mat();

        /**************************************************************************************** */
        Calib3d.Rodrigues(angleZVector, Rz);
        Calib3d.Rodrigues(angleXVector, Rx);
        Calib3d.Rodrigues(angleYVector, Ry);
        /**************************************************************************************** */

        // logger.debug("Rz\n" + Rz.dump());
        // logger.debug("Rx\n" + Rx.dump());
        // logger.debug("Ry\n" + Ry.dump());

        // in Python (Ry).dot(Rx).dot(Rz) messed up nomenclature - it's often really matrix multiply Ry times Rx times Rz
        Mat R = Mat.eye(4, 4, CvType.CV_64FC1);
        Mat R3x3 = R.submat(0, 3, 0, 3);

        /**************************************************************************************** */
        Core.gemm(Ry, Rx, 1., new Mat(), 0, R3x3);
        Core.gemm(R3x3, Rz, 1., new Mat(), 0., R3x3); // rotation matrix of the input Euler Angles [radians]
        /**************************************************************************************** */
        
        angleZVector.release();
        angleXVector.release();
        angleYVector.release();
        Rz.release();
        Rx.release();
        Ry.release();

        // translate board to its center
        Mat Tc = Mat.eye(4, 4, CvType.CV_64FC1);
        Mat Tc1x3 = Tc.submat(3, 4, 0, 3);
        Mat Tc3x1 = new Mat();
        Mat translateToBoardCenter = new Mat(bbox.rows(), bbox.cols(), bbox.type()); // matching bbox for element by element multiply
        translateToBoardCenter.put(0, 0, -0.5, -0.5, 0.);
        translateToBoardCenter = bbox.mul(translateToBoardCenter);
        // logger.debug("translateToBoardCenter\n" + translateToBoardCenter.dump());

        // logger.debug("R " + R.dump());
        // logger.debug("R3x3 " + R3x3.dump());
        // logger.debug("Tc " + Tc.dump());
        // logger.debug("Tc1x3 " + Tc1x3.dump());

        /*************************************************************************************** */
        Core.gemm(R3x3, translateToBoardCenter, 1., new Mat(), 0.,Tc3x1);
        Tc3x1.t().copyTo(Tc1x3); // update Tc
        /*************************************************************************************** */
        // logger.debug("Tc " + Tc.dump());
        // logger.debug("Tc1x3 " + Tc1x3.dump());

        // translate board to center of image

        Mat T = Mat.eye(4, 4, CvType.CV_64FC1);
        Mat T1x3 = T.submat(3, 4, 0, 3);     
        /*************************************************************************************** */
        Mat translateToImageCenter = new Mat(bbox.rows(), bbox.cols(), bbox.type()); // matching bbox for element by element multiply
        translateToImageCenter.put(0, 0, -0.5, -0.5, Z);
        bbox.mul(translateToImageCenter).t().copyTo(T1x3);   
        /*************************************************************************************** */
        // logger.debug("translateToImageCenter " + translateToImageCenter.dump());
        // logger.debug("T1x3 " + T1x3.dump());
        // logger.debug("T " + T.dump());

        // rotate center of board
        Mat Rf = new Mat();
        
        /*************************************************************************************** */
        Core.gemm(Tc.inv(), R, 1., new Mat(), 0.,Rf);
        Core.gemm(Rf, Tc, 1., new Mat(), 0.,Rf);
        Core.gemm(Rf, T, 1., new Mat(), 0.,Rf);
        /*************************************************************************************** */
        // logger.debug("Rf " + Rf.dump());

        // return cv2.Rodrigues(Rf[:3, :3])[0].ravel(), Rf[3, :3]
        Mat Rf3x3 = Rf.submat(0, 3, 0, 3);
        Mat RfVector = new Mat(1, 3, CvType.CV_64FC1);

        /*************************************************************************************** */
        Calib3d.Rodrigues(Rf3x3,RfVector);
        Core.transpose(RfVector, RfVector);
        /*************************************************************************************** */
        // logger.debug("RfVector returned " + RfVector.dump());

        Mat t = Rf.submat(3, 4, 0, 3);
        Mat tVector = new Mat();
        t.copyTo(tVector);
        // logger.debug("tVector returned " + tVector.dump());

        Tc.release();
        Tc1x3.release();
        translateToBoardCenter.release();
        translateToImageCenter.release();
        T.release();
        T1x3.release();
        Rf.release();
        Rf3x3.release();
        t.release();

        List<Mat> rt = new ArrayList<Mat>(2);
        rt.add(RfVector);
        rt.add(tVector);

        return rt;
    }
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
/*                                                                                                 */
/*                                     pose_planar_fullscreen                                      */
/*                                     pose_planar_fullscreen                                      */
/*                                     pose_planar_fullscreen                                      */
/*                                                                                                 */
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
    private static List<Mat> pose_planar_fullscreen(Mat K, Mat cdist, Size img_size, Mat bbox)
    {
        // logger.debug("method entered  . . . . . . . . . . . . . . . . . . . . . . . .");

        // don't use the principal point throughout just have X and Y no Z until it's calculated in the middle
        // compute a new Z
        // logger.debug("camera matrix K " + K + "\n" + K.dump());
        // logger.debug("cdist " + cdist.dump());
        // logger.debug("img_size " + img_size.toString());
        // logger.debug("bbox " + bbox + "\n" + bbox.dump());

        Mat KB = new Mat(); // ignore principal point
        Mat bboxZeroZ = new Mat(3, 1, CvType.CV_64FC1);
        bboxZeroZ.put(0, 0, bbox.get(0, 0)[0], bbox.get(1, 0)[0], 0.);

        Core.gemm(K, bboxZeroZ,1., new Mat(), 0., KB);

        double KBx = KB.get(0, 0)[0];
        double KBy = KB.get(1, 0)[0];
        double Z = Math.min(KBx/img_size.width, KBy/img_size.height);
        double[] pB = {KBx/Z, KBy/Z};

        Mat r = new Mat(1, 3, CvType.CV_64FC1);
        r.put(0, 0, Math.PI, 0., 0.); // flip image

        // move board to center, org = bl
        MatOfPoint2f p = new MatOfPoint2f(new Point(img_size.width/2. - pB[0]/2., img_size.height/2. + pB[1]/2.));
        
        Mat t = unproject(p, K, cdist, Z);

        // logger.debug("KBnoPrinciplePoint " + KB + KB.dump());
        // logger.debug("Z, pB(x, y) " + Z + ", " + java.util.Arrays.toString(pB));
        // logger.debug("p " + p + p.dump());
        // logger.debug("returning r " + r + r.dump());
        // logger.debug("returning t " + t + t.dump());

        bboxZeroZ.release();
        KB.release();
        p.release();

        List<Mat> rt = new ArrayList<>(2);
        rt.add(r); // don't release r; this is the object that is returned
        rt.add(t); // don't release t; this is the object that is returned
        return rt;
    }
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
/*                                                                                                 */
/*                                     pose_from_bounds                                            */
/*                                     pose_from_bounds                                            */
/*                                     pose_from_bounds                                            */
/*                                                                                                 */
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
    /**
    * 
    * @param src_extParm
    * @param tgt_rect
    * @param K
    * @param cdist
    * @param img_sz
    * @return
    */
    private pose_from_boundsReturn pose_from_bounds(Mat src_extParm, Rect tgt_rect, Mat K, Mat cdist, Size img_sz)
    {
        // logger.debug("method entered  . . . . . . . . . . . . . . . . . . . . . . . .");

        // logger.debug("src_extParm " + src_extParm + "\n" + src_extParm.dump()); // full ChArUcoBoard size + Z
        // logger.debug("tgt_rect " + tgt_rect); // guidance board posed
        // logger.debug("camera matrix K " + K + "\n" + K.dump());
        // logger.debug("cdist " + cdist.dump());
        // logger.debug("img_sz " + img_sz.toString()); // camera screen image size

        double[] src_ext = new double[(int)src_extParm.total()];
        src_extParm.get(0, 0, src_ext);

        boolean rot90 = tgt_rect.height > tgt_rect.width;

        int MIN_WIDTH = (int)Math.floor(img_sz.width/3.333); // posed guidance board must be about a third or more of the camera, screen size

        // logger.debug("rot90 " + rot90);

        if (rot90)
        {
            // flip width and height
            // src_ext = src_ext.clone(); // cloning not needed in this implementation since already hiding parameter src_extParm
            double swapWH = src_ext[0];
            src_ext[0] = src_ext[1];
            src_ext[1] = swapWH;

            if (tgt_rect.height < MIN_WIDTH)
            {
                // double scale = MIN_WIDTH / tgt_rect.width; //FIXME is this wrong in original? tgt_rect.width => tgt_rect.height? - rkt
                double scale = MIN_WIDTH / tgt_rect.height;
                tgt_rect.height = MIN_WIDTH;
                tgt_rect.width *= scale;
            }
        }
        else
        {
            if (tgt_rect.width < MIN_WIDTH)
            {
                double scale = MIN_WIDTH / tgt_rect.width;
                tgt_rect.width = MIN_WIDTH;
                tgt_rect.height *= scale;
            }
        }
        double aspect = src_ext[0] / src_ext[1]; // w/h of the full ChArUcoBoard // [2520; 1680; 2520] => 1.5

        // match aspect ratio of tgt to src, but keep tl
        if ( ! rot90)
        {
            tgt_rect.height = (int)(tgt_rect.width / aspect); // adapt height
        }
        else
        {
            tgt_rect.width = (int)(tgt_rect.height * aspect); // adapt width
        }
        // logic error here (missing check), I'm sure, so fix it - rkt
        // if target too wide reduce to image size; if target too high reduce to image size
        if (tgt_rect.width > img_sz.width)
        {
            aspect = img_sz.width/tgt_rect.width;
            tgt_rect.width = (int)(tgt_rect.height * aspect); // adapt width            
            tgt_rect.height = (int)(tgt_rect.width * aspect); // adapt height
        }
        if (tgt_rect.height > img_sz.height)
        {
            aspect = img_sz.height/tgt_rect.height;
            tgt_rect.width = (int)(tgt_rect.height * aspect); // adapt width            
            tgt_rect.height = (int)(tgt_rect.width * aspect); // adapt height
        }

        Mat r = new Mat(1, 3, CvType.CV_64FC1);
        r.put(0, 0, Math.PI, 0., 0.);
        
        // org is bl (bottom left)
        if (rot90)
        {
            Mat R = new Mat();

            Calib3d.Rodrigues(r, R);

            // logger.debug("R " + R.dump());
            Mat rz = new Mat(1, 3, CvType.CV_64FC1);
            rz.put(0, 0, 0., 0., -Math.PI/2.);
            Mat Rz = new Mat();

            Calib3d.Rodrigues(rz, Rz);

            // logger.debug("Rz " + Rz.dump());

            Core.gemm(R, Rz, 1., new Mat(), 0., R); // rotation matrix of the input Euler Angles

            Calib3d.Rodrigues(R, r);

            r = r.t(); // (ravel) Rodrigues out is 3x1 and (most of) the rest of program is 1x3

            R.release();
            Rz.release();
            rz.release();
            // org is tl (top left)
        }

        double Z = (K.get(0, 0)[0] * src_ext[0]) / tgt_rect.width;
        // logger.debug("before clip tgt_rect " + tgt_rect);

        //  clip to image region
        int[] min_off = {0, 0};
        int[] max_off = {(int)(img_sz.width - tgt_rect.width), (int)(img_sz.height - tgt_rect.height)};
        tgt_rect.x = Math.min(max_off[0], Math.max(tgt_rect.x, min_off[0]));
        tgt_rect.y = Math.min(max_off[1], Math.max(tgt_rect.y, min_off[1]));
        // logger.debug("after clip tgt_rect " + tgt_rect);

        if ( ! rot90)
        {
            tgt_rect.y += tgt_rect.height;
        }

        MatOfPoint2f p = new MatOfPoint2f(new Point(tgt_rect.x, tgt_rect.y));

        Mat t = unproject(p, K, cdist, Z);

        if ( ! rot90)
        {
            tgt_rect.y -= tgt_rect.height;
        }

        // logger.debug("returning r " + r.dump());
        // logger.debug("returning t " + t.dump());
        // logger.debug("returning tgt_rect " + tgt_rect);

        return new pose_from_boundsReturn(r, t, tgt_rect);
    }
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
/*                                                                                                 */
/*                                     PoseGeneratorDist Constructor                               */
/*                                     PoseGeneratorDist Constructor                               */
/*                                     PoseGeneratorDist Constructor                               */
/*                                                                                                 */
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
    // two angle bins - one for the x axis [0] and one for the y axis [1]
    private Gen_Bin[] gb = {new Gen_Bin(Math.toRadians(-70.), Math.toRadians(70.)),  // valid poses: r_x -> -70° .. 70°
                            new Gen_Bin(Math.toRadians(-70.), Math.toRadians(70.))}; // valid poses: r_y -> -70° .. 70°
    /**
    * getter for the angle bins per axis
    * 
    * @param axis x = 0; y = 1
    * @return next angle iteration for the selected axis
    */
    private Function<Integer, double[]> orbital = (axis) ->
    {
        // !!!! NOT a normal function - it returns a different value each time
        // from the angle binning "iterator"
        /*
        * generate next angles
        *   {next x ,      0} for the x axis [0]
        * OR 
        *   {     0 , next y} for the y axis [1]
        */
        double[] angle = { 0., 0.};
        angle[axis] = gb[axis].next();
        return angle;
    };

    private static final int SUBSAMPLE = 20; // there is not a good conversion in general for this type of Python variable. Assuming value doesn't change nor instantiated more than once and changed elsewhere this conversion works.
    private Size img_size;
    // self.stats = [1, 1]  # number of (intrinsic, distortion) poses -- NOT USED
    private double orbitalZ = 1.6;
    private static final double rz = Math.PI / 8.;

    private Mat mask;
    private double sgn = 1.;

 /**
 *     generate poses based on min/ max distortion
 * @param img_size
 */
    PoseGeneratorDist(Size img_size)
    {
        logger.debug("Starting ----------------------------------------");

        this.img_size = img_size;
        mask = Mat.zeros(
            new Size(Math.floor(img_size.width/this.SUBSAMPLE), Math.floor(img_size.height/this.SUBSAMPLE)),
            CvType.CV_8UC1); // t() transpose not needed in Java since those bindings account for w-h reversal
    }
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
/*                                                                                                 */
/*                                     compute_distortion                                          */
/*                                     compute_distortion                                          */
/*                                     compute_distortion                                          */
/*                                                                                                 */
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
    private List<Mat> compute_distortion(Mat K, Mat cdist, int subsample)
    {
        // logger.debug("method entered  . . . . . . . . . . . . . . . . . . . . . . . .");

        return Distortion.sparse_undistort_map(K, img_size, cdist, K, subsample);
    }
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
/*                                                                                                 */
/*                                     get_pose                                                    */
/*                                     get_pose                                                    */
/*                                     get_pose                                                    */
/*                                                                                                 */
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
/*
 * Determines (returns) the rotation and translation vectors to apply to the guidance board to effect
 * the desired pose that was determined to collect information to converge on the correct intrinsics.
 * 
 * @param bbox bounding box size of the calibration pattern
 * @param nk number of keyframes captured so far
 * @param tgt_param intrinic number of parameter that should be optimized by the pose
 * @param K camera matrix current calibration estimate
 * @param cdist distortion coefficients current calibration estimate
 * @return rotation vector and translation vector
 * @throws Exception
 */
    List<Mat> get_pose(Mat bbox, int nk, int tgt_param, Mat K, Mat cdist)
    {
        // logger.debug("method entered  . . . . . . . . . . . . . . . . . . . . . . . .");
        // logger.debug("bbox " + bbox + "\n" + bbox.dump());
        // logger.debug("nk " + nk);
        // logger.debug("tgt_param " + tgt_param);
        // logger.debug("camera matrix K " + K + "\n" + K.dump());
        // logger.debug("cdist " + cdist.dump());

        // first frame will be orbital pose from fixed angles
        if (nk == 0)
        {
            // x is camera pointing ahead up/down; y is camera pointing left/right; z is camera rotated (Z is axis from camera to target)
            // init sequence: first keyframe  0° flat - not up or down, 45° pointing left, 22.5° rotated CW

            /********************************************************************************************************* */
            return orbital_pose(bbox, 0., Math.PI/4., this.orbitalZ, Math.PI/8./*probably this.rz*/);
            /********************************************************************************************************* */
        }

        //second frame will be full screen planar based on the K estimated from the first frame
        if (nk == 1)
        {
            // init sequence: second keyframe

            /********************************************************************************************************* */
            return pose_planar_fullscreen(K, cdist, this.img_size, bbox);
            /********************************************************************************************************* */
        }

        // poses for all the frames after the first two
        // pose will be based on the parameter being analyzed
        if (tgt_param < 4)
        {
            // orbital pose is used for focal length
            int axis = (tgt_param + 1) % 2;  // f_y -> r_x
            
            // r, t = orbital_pose(bbox, *next(self.orbital[axis]));

            double[] angleIteration = this.orbital.apply(axis); // get the next iteration for x & y pair of angles for the given axis
            // logger.debug("angleIteration " + java.util.Arrays.toString(angleIteration));

            /********************************************************************************************************* */
            List<Mat> rt = orbital_pose(bbox, angleIteration[0], angleIteration[1], this.orbitalZ, this.rz);
            /********************************************************************************************************* */
            
            // logger.debug("rt " + rt.get(0).dump() + rt.get(1).dump());
            
            Mat t = rt.get(1);

            if (tgt_param > 1)
            {
                // nudge the principal point in the axis in process and unproject it
                // "off" is Principal Point Cx, Cy from the intrinsics camera matrix
                double[] offPPoint = {K.get(0, 2)[0], K.get(1, 2)[0]};
                offPPoint[tgt_param - 2] += ((tgt_param - 2) == 0 ? this.img_size.width : this.img_size.height) * 0.05 * this.sgn;
                MatOfPoint2f off = new MatOfPoint2f(new Point(offPPoint));

                /********************************************************************************************************* */
                Mat off3d = unproject(off, K, cdist, t.get(0, 2)[0]);
                /********************************************************************************************************* */

                off3d.put(0, 2, 0.); // zero out the Z
                Core.add(t, off3d, t); // pretty big offset being nearly the principal point
                this.sgn = -this.sgn;
                off.release();
                off3d.release();
            }

            rt.set(1, t); // update the nudged t and return r and t

            // logger.debug("returning rt " + rt.get(0).dump() + rt.get(1).dump());

            return rt;
        }

        /********************************************************************************************************* */
        List<Mat> res = compute_distortion(K, cdist, this.SUBSAMPLE);
        /********************************************************************************************************* */

        Mat dpts = res.get(0);
        Mat pts = res.get(1);

        /********************************************************************************************************* */
        Rect bounds = Distortion.loc_from_dist(pts, dpts, this.mask, false, 1.); // ignore previously used masked off areas
        /********************************************************************************************************* */

        if (bounds == null)
        {
            logger.error("bounds is null, pose not contributing; guessing what to do");
            return get_pose(bbox, nk, 3, K, cdist); // best guess of what the author meant to do (drop the axis)
        }

        dpts.release();
        pts.release();

        Rect tgt_rect = new Rect(bounds.x*this.SUBSAMPLE, bounds.y*this.SUBSAMPLE, bounds.width*this.SUBSAMPLE, bounds.height*this.SUBSAMPLE);
        /********************************************************************************************************* */
        pose_from_boundsReturn res2 = pose_from_bounds(bbox, tgt_rect, K, cdist, this.img_size);
        /********************************************************************************************************* */
        Mat r = res2.r;
        Mat t = res2.t;
        Rect nbounds = res2.tgt_rect;

        // logger.debug("returning r " + r.dump());
        // logger.debug("returning t " + t.dump());
        // logger.debug("nbounds " + nbounds);
   
        nbounds.x = (int)Math.ceil((double)nbounds.x / this.SUBSAMPLE);
        nbounds.y = (int)Math.ceil((double)nbounds.y / this.SUBSAMPLE);
        nbounds.width = (int)Math.ceil((double)nbounds.width / this.SUBSAMPLE);
        nbounds.height = (int)Math.ceil((double)nbounds.height / this.SUBSAMPLE);

        // The np.ceil of the scalar x is the smallest integer i, such that i >= x
        // mask off y through y+h(-1) and x through x+w(-1)
        Mat.ones(nbounds.height, nbounds.width, this.mask.type())
            .copyTo(this.mask.submat(nbounds.y, nbounds.y+nbounds.height, nbounds.x, nbounds.x+nbounds.width));

        // logger.debug("mask count non-zeros = " + Core.countNonZero(this.mask) + "\n" + ArrayUtils.brief(this.mask));

        List<Mat> rt = new ArrayList<>(2);
        rt.add(r);
        rt.add(t);

        return rt;
    }
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
/*                                                                                                 */
/*                                     pose_from_boundsReturn class                                */
/*                                     pose_from_boundsReturn class                                */
/*                                     pose_from_boundsReturn class                                */
/*                                                                                                 */
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
    /**
     * container to return multiple values from pose_from_bounds
     */
    class pose_from_boundsReturn
    {
        Mat r;
        Mat t;
        Rect tgt_rect;
        pose_from_boundsReturn(Mat r, Mat t, Rect tgt_rect)
        {
            this.r = r;
            this.t = t;
            this.tgt_rect = tgt_rect;
        }         
    }
}
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
/*                                                                                                 */
/*                                     End PoseGenerator class                                     */
/*                                     End PoseGenerator class                                     */
/*                                     End PoseGenerator class                                     */
/*                                                                                                 */
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/

// Gen_Bin unit test
// PoseGeneratorDist pgd = new PoseGeneratorDist(new Size(1280., 720.));
// // Gen_Bin gb = pgd.new Gen_Bin(Math.toRadians(-70.), Math.toRadians(70.));
// Gen_Bin gb = pgd.new Gen_Bin(0., 1.);
// for (int i = 0; i < 40; i++)
// {
//     System.out.println(gb.next());
// }
// System.exit(0);

// OpenCV uses reference counting.
// submat adds another reference to the data memory.
// .release() does not deallocate the memory, unless the last reference was removed/decremented.
