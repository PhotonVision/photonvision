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

// projects a 2D object (image) according to parameters - generate styled board image
package org.photonvision.calibrator;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
/*                                                                                                 */
/*                                     BoardPreview class                                          */
/*                                     BoardPreview class                                          */
/*                                     BoardPreview class                                          */
/*                                                                                                 */
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
class BoardPreview {
    private static final Logger logger = new Logger(BoardPreview.class, LogGroup.General);

    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    /*                                                                                                 */
    /*                                     project_img                                                 */
    /*                                     project_img                                                 */
    /*                                     project_img                                                 */
    /*                                                                                                 */
    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    /**
     * @param img image to project
     * @param sz size of the final image
     * @param K
     * @param rvec
     * @param t
     * @param flags
     * @return
     */
    private static Mat project_img(Mat img, Size sz, Mat K, Mat rvec, Mat t, int flags)
                // force user to specify flags=cv2.INTER_LINEAR to use default
            {
        // logger.debug("method entered  . . . . . . . . . . . . . . . . . . . . . . . .");
        // logger.debug("img " + img);
        // logger.debug("sz " + sz);
        // logger.debug("K " + K + "\n" + K.dump());
        // logger.debug("rvec " + rvec + rvec.dump());
        // logger.debug("t " + t + t.dump());
        // logger.debug("flags " + flags);

        // construct homography
        Mat R = new Mat();
        Calib3d.Rodrigues(rvec, R);

        // logger.debug("R " + R + "\n" + R.dump());
        Mat transform =
                new Mat(3, 3, CvType.CV_64FC1); // rotation matrix R and a translation matrix T (t)
        transform.put(
                0,
                0,
                R.get(0, 0)[0],
                R.get(0, 1)[0],
                R.get(0, 2)[0], // 1st row r,second row r, third row t
                R.get(1, 0)[0],
                R.get(1, 1)[0],
                R.get(1, 2)[0],
                t.get(0, 0)[0],
                t.get(0, 1)[0],
                t.get(0, 2)[0]);
        Core.transpose(transform, transform);
        Mat H = new Mat();
        Core.gemm(K, transform, 1., new Mat(), 0., H);
        Core.divide(H, new Scalar(H.get(2, 2)[0]), H);

        // logger.debug("transform " + transform + "\n" + transform.dump());
        // logger.debug("R " + R + "\n" + R.dump());
        // logger.debug("H " + H + "\n" + H.dump());

        Mat imgProjected = new Mat();

        Imgproc.warpPerspective(img, imgProjected, H, sz, flags);

        // draw axes on the warped Guidance Board
        // these are drawn in the wrong corner and drawing before the warpPerspective doesn't work -
        // tiny image in worse spot
        // these axes diagram cover the desired posed (warped) Guidance Board before it is processed.
        // Maybe draw them later and they are in a different position
        Core.flip(
                imgProjected,
                imgProjected,
                0); // flip to get axes origin in correct corner BUT the estimated origin is reversed from
        // where it belongs
        Calib3d.drawFrameAxes(
                imgProjected, K, new Mat(), R, t, 300.f); // may need rotation vector rvec instead of R
        Core.flip(imgProjected, imgProjected, 0); // put the img back right

        transform.release();
        R.release();
        H.release();

        // logger.debug("returning imgProjected\n" + ArrayUtils.brief(imgProjected));

        return imgProjected;
    }

    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    /*                                                                                                 */
    /*                                     BoardPreview constructor                                    */
    /*                                     BoardPreview constructor                                    */
    /*                                     BoardPreview constructor                                    */
    /*                                                                                                 */
    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    private Size SIZE =
            new Size(
                    640.,
                    480.); // different than camera res okay and it runs a little faster if smaller. Resize at
    // end makes images match
    private Size sz;
    private Mat img = new Mat();
    private Mat shadow; // used for overlap score
    private Mat maps; // 2D version used for remap function
    private Mat Knew = new Mat();

    BoardPreview(Mat img) {
        logger.debug("Starting ----------------------------------------");

        img.copyTo(this.img);

        // at this point the img appears correctly if displayed on a screen or printed
        Core.flip(this.img, this.img, 0); // flipped when printing
        // at this point the img is flipped upside down. This is needed because the
        // Imgproc.warpPerspective
        // flips the img upside down likely because it's acting like it's projecting through a camera
        // aperture/lens which flips the scene upside down. So after the Imgproc.warpPerspective the img
        // is
        // again upside right.

        // light to much to see low exposure camera images behind it so reduce almost to nothing. Can't
        // set to 0 - messes up other places that check 0 or not 0
        // set black pixels to gray; non-black pixels stay the same

        // process entire Mat for efficiency
        byte[] img1ChannelBuff =
                new byte
                        [this.img.rows()
                                * this.img.cols()
                                * this.img.channels()]; // temp buffer for more efficient access
        this.img.get(0, 0, img1ChannelBuff); // get the row, all channels
        for (int index = 0; index < img1ChannelBuff.length; index++) // process each element of the row
        {
            // if the camera image is dimmer then the guidance board needs to be dimmer.
            // if the camera image is bright then the guidance board needs to be bright.
            if (img1ChannelBuff[index] == 0) // is it black?
            {
                img1ChannelBuff[index] =
                        Cfg.guidanceBlack; // 0 messes up the shadow board logic that relies on non-zero
                // pixels. Need major surgery to fix
            } else {
                img1ChannelBuff[index] = Cfg.guidanceWhite;
            }
        }
        this.img.put(0, 0, img1ChannelBuff);

        Imgproc.cvtColor(this.img, this.img, Imgproc.COLOR_GRAY2BGR);

        // set blue and red channels to black (0) so gray/white becomes a shade of green (green channel
        // was not changed)
        byte[] img3ChannelBuff =
                new byte
                        [this.img.rows()
                                * this.img.cols()
                                * this.img.channels()]; // temp buffers for efficient access
        // process one row at a time for efficiency
        this.img.get(0, 0, img3ChannelBuff); // get the row, all channels
        for (int index = 0;
                index < img3ChannelBuff.length;
                index += 3) // process each triplet (channels) of the row
        {
            img3ChannelBuff[index] = 0; // B
            img3ChannelBuff[index + 2] = 0; // R
        }
        this.img.put(0, 0, img3ChannelBuff);

        // used for overlap score
        this.shadow = Mat.ones(this.img.rows(), this.img.cols(), CvType.CV_8UC1);
    }

    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    /*                                                                                                 */
    /*                                     create_maps                                                 */
    /*                                     create_maps                                                 */
    /*                                     create_maps                                                 */
    /*                                                                                                 */
    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    void create_maps(Mat K, Mat cdist, Size sz) {
        // logger.debug("method entered  . . . . . . . . . . . . . . . . . . . . . . . .");
        // logger.debug("camera matrix K " + K + "\n" + K.dump());
        // logger.debug("cdist " + cdist.dump());
        // logger.debug("sz " + sz);

        // cdist initialized in its constructor instead of setting to 0 here if null; did 5 not 4 for
        // consistency with rest of code
        this.sz = sz;
        Mat scale = Mat.zeros(3, 3, K.type()); // assuming it's K.rows(), K.cols()
        scale.put(0, 0, this.SIZE.width / sz.width);
        scale.put(1, 1, this.SIZE.height / sz.height);
        scale.put(2, 2, 1.);
        // Core.gemm(scale, K, 1., new Mat(), 0.,K);
        // FIXME Knew and K are suspect. What's the right answer to making Knew without trashing K?
        // Guessing here but seems okay.
        Core.gemm(scale, K, 1., new Mat(), 0., this.Knew);
        sz = this.SIZE;
        this.Knew =
                Calib3d.getOptimalNewCameraMatrix(
                        Knew, cdist, sz, 1.); // .2% higher than older Python OpenCV for same input

        this.maps = Distortion.make_distort_map(Knew, sz, cdist, this.Knew);
    }

    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    /*                                                                                                 */
    /*                                     project                                                     */
    /*                                     project                                                     */
    /*                                     project                                                     */
    /*                                                                                                 */
    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    Mat project(Mat r, Mat t, boolean useShadow, int inter)
                // force users to specify useShadow=false and inter=Imgproc.INTER_NEAREST instead of
                // defaulting
                // no default allowed in Java and I don't feel like making a bunch of overloaded methods for
                // this conversion
            {
        // logger.debug("method entered  . . . . . . . . . . . . . . . . . . . . . . . .");
        // logger.debug("r " + r.dump());
        // logger.debug("t " + t.dump());
        // logger.debug("useShadow " + useShadow);
        // logger.debug("inter " + inter);
        // logger.debug("sz " + this.sz);

        Mat img = new Mat();

        img =
                project_img(
                        useShadow ? this.shadow : this.img, this.SIZE, this.Knew, r, t, Imgproc.INTER_LINEAR);

        // logger.debug("maps " + this.maps + "\n" + ArrayUtils.brief(maps));
        // Can be one map for XY or two maps X and Y. python had 2 and this has 1
        // Imgproc.remap(img, img, maps[0]/*X*/, maps[1]/*Y*/, inter);// maybe X Mat and Y Mat somehow;
        // separate channels?

        Imgproc.remap(
                img, img, this.maps, new Mat(),
                inter); // 1st arg can be XY with no 2nd arg (original has separate X and Y arguments)
        // logger.debug("img after remap " + img + "\n" + ArrayUtils.brief(img));

        // maps (2, 480, 640)
        Imgproc.resize(img, img, this.sz, 0, 0, inter);

        // logger.debug("returning img after resize " + img + "\n" + ArrayUtils.brief(img));

        return img;
    }
}
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
/*                                                                                                 */
/*                                     End BoardPreview class                                      */
/*                                     End BoardPreview class                                      */
/*                                     End BoardPreview class                                      */
/*                                                                                                 */
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
