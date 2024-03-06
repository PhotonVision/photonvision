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

// Calibrate Camera with efficient camera Pose Guidance provided on screen to the user

package org.photonvision.vision.pipe.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.LogLevel;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.frame.FrameDivisor;
import org.photonvision.vision.pipe.CVPipe;
import org.photonvision.vision.pipe.impl.Calibrate3dPoseGuidance.Cfg;
import org.photonvision.vision.pipe.impl.Calibrate3dPoseGuidance.ChArucoDetector;
import org.photonvision.vision.pipe.impl.Calibrate3dPoseGuidance.UserGuidance;
import org.photonvision.vision.pipe.impl.Calibrate3dPoseGuidance.keyframe;
import org.photonvision.vision.pipeline.UICalibrationData;

/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
/*                                                                                                 */
/*                                     FindBoardCornersGuidancePipe class                          */
/*                                     FindBoardCornersGuidancePipe class                          */
/*                                     FindBoardCornersGuidancePipe class                          */
/*                                                                                                 */
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
public class FindBoardCornersGuidancePipe
        extends CVPipe<
                Pair<Mat, Mat>,
                FindBoardCornersGuidancePipe.FindBoardCornersGuidancePipeResult,
                FindBoardCornersGuidancePipe.FindCornersGuidancePipeParams> {

    private static final String VERSION = "beta 12.12"; // change this

    static final Logger logger = new Logger(FindBoardCornersGuidancePipe.class, LogGroup.General);

    {
        Logger.setLevel(LogGroup.General, LogLevel.DEBUG);
        logger.info("Pose Guidance Camera Calibration version " + VERSION);

        // Path to save images
        // final Path imageDir = ConfigManager.getInstance().getCalibDir();
    }
    Mat progressInsert;
    
    Size img_size;
    Size img_size_start;
    boolean firstFrame = true; // must be managed at the end of each calibration session
    ChArucoDetector tracker;

    UserGuidance ugui;

    // runtime variables
    boolean mirror;
    boolean save; // indicator for user pressed the "c" key to capture (save) manually
    String endMessage; // status of calibration at the end of a frame

    int frameNumber;

    FindBoardCornersGuidancePipeResult findBoardCornersGuidancePipeResult;
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
/*                                                                                                 */
/*                                     FindBoardCornersGuidancePipe constructor                    */
/*                                     FindBoardCornersGuidancePipe constructor                    */
/*                                     FindBoardCornersGuidancePipe constructor                    */
/*                                                                                                 */
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
    // public FindBoardCornersGuidancePipe()
    // {

    // }

    /**
     * Finds the corners in a given image and returns them
     *
     * @param in Input for pipe processing. Pair of input and output mat
     * @return All valid Mats for camera calibration
     */
    @Override
    protected FindBoardCornersGuidancePipeResult process(Pair<Mat, Mat> in) {

        this.img_size = new Size(in.getLeft().width(), in.getLeft().height());

        if ( ! createGuidance()) {
            findBoardCornersGuidancePipeResult.takeSnapshot = false;
            findBoardCornersGuidancePipeResult.haveEnough = false;
            findBoardCornersGuidancePipeResult.cancelCalibration = true;
            // detected corners are irrelevant and should be ignored but could be emptied here if desired
            return findBoardCornersGuidancePipeResult;
        }

        return findBoardCornersGuidance(in);
    }
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
/*                                                                                                 */
/*                                     createGuidance                                              */
/*                                     createGuidance                                              */
/*                                     createGuidance                                              */
/*                                                                                                 */
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
    public boolean createGuidance() {

        // first time through processing and verify resolution; effectively the constructor for a calibration
   
        if (this.firstFrame) {
            img_size_start = img_size.clone(); // set resolution; all frames must match this first
            tracker = new ChArucoDetector(this.img_size);
            ugui = new UserGuidance(tracker, Cfg.var_terminate, this.img_size);
            mirror = false; // indicator for user pressed the "m" key to present mirrored view
            frameNumber = 0;
            findBoardCornersGuidancePipeResult = new FindBoardCornersGuidancePipeResult();
            firstFrame = false;
        }

        // check all future frames against the first frame size
        if ( ! img_size_start.equals(this.img_size)) {
            logger.warn("changing image size to " + this.img_size + " from " + img_size_start + ", canceling calibration");
            return false; // resolution changed during calibration; that's bad
        }

        return true;
    }
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
/*                                                                                                 */
/*                                     findBoardCornersGuidance                                    */
/*                                     findBoardCornersGuidance                                    */
/*                                     findBoardCornersGuidance                                    */
/*                                                                                                 */
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
    public  FindBoardCornersGuidancePipeResult findBoardCornersGuidance(Pair<Mat, Mat> in)
    {
        // resets for every frame
        endMessage = "not the end";
        progressInsert = new Mat();
        save = this.params.save; // indicator for user pressed the "c" key to capture (save) manually
        Mat imgPV = in.getLeft();
        Mat outPV = in.getRight();

        Mat img = new Mat();
        imgPV.copyTo(img);

        Mat out = new Mat(); // user display Mat

        frameNumber++;
        if (frameNumber%Cfg.garbageCollectionFrames == 0) // periodically cleanup old Mats
        {
            System.runFinalization();
            System.gc();
        }
        boolean force = false;  // force add frame to calibration (no support yet still images else (force = !live)

        boolean fewCorners = tracker.detect(img); // detect the board

        if (save)
        {
            save = false;
            force = true;
        }

        img.copyTo(out); // out has the camera image at his point

        try {
            ugui.draw(out, mirror); // this adds the guidance board to the camera image (out) to make the new out
        } catch (Exception e) {
            e.printStackTrace();
            endMessage = "CANCELLED";
        }

        boolean capturedPose = ugui.update(force, progressInsert); // calibrate
        
        if (capturedPose)
        {
            endMessage = "CAPTURED";
        }

        displayOverlay(out, ugui, fewCorners, frameNumber, progressInsert);

        out.copyTo(outPV);

        if (ugui.converged()) // are we there yet?
        {
            ugui.calib.calibrate(new ArrayList<>(1)); // final, dummy arg to use all captures

            ugui.write(); // write all the calibration data

            endMessage = "CALIBRATED";
        }

        if ( ! endMessage.equals("not the end"))
        {
            logger.info("Pose Guidance Camera action " + endMessage);
        }

        out.copyTo(outPV);

        // most frames will return falses since nothing special happened
        findBoardCornersGuidancePipeResult.takeSnapshot = false;
        findBoardCornersGuidancePipeResult.haveEnough = false;
        findBoardCornersGuidancePipeResult.cancelCalibration = false;

        if (endMessage.equals("CALIBRATED"))
        {
            findBoardCornersGuidancePipeResult.takeSnapshot = true;
            findBoardCornersGuidancePipeResult.haveEnough = true;
            firstFrame = true; // reset first time switch for a following calibration
        }
        else    
        if (endMessage.equals("CAPTURED"))
        {
            findBoardCornersGuidancePipeResult.takeSnapshot = true;
        }
        else
        if (endMessage.equals("CANCELLED"))
        {
            findBoardCornersGuidancePipeResult.cancelCalibration = true;
            firstFrame = true; // reset first time switch for a following calibration
        }

        if (findBoardCornersGuidancePipeResult.takeSnapshot == true)
        {
            keyframe snapshot = tracker.get_calib_pts();
            findBoardCornersGuidancePipeResult.objCorners = snapshot.p3d().clone();
            findBoardCornersGuidancePipeResult.imgCorners = snapshot.p2d().clone();
            findBoardCornersGuidancePipeResult.idCorners = snapshot.pid().clone();
        }
        else
        {
            findBoardCornersGuidancePipeResult.objCorners = null;
            findBoardCornersGuidancePipeResult.imgCorners = null;
            findBoardCornersGuidancePipeResult.idCorners = null;
        }

        out.copyTo(outPV);

        return findBoardCornersGuidancePipeResult;
    }   
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
/*                                                                                                 */
/*                                     displayOverlay                                              */
/*                                     displayOverlay                                              */
/*                                     displayOverlay                                              */
/*                                                                                                 */
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
    public void displayOverlay(Mat out, UserGuidance ugui, boolean fewCorners, int frameNumber, Mat progressInsert)
    {
        String frame = String.format("%05d ", frameNumber);
        Imgproc.putText(out, frame, new Point(0, 20), Imgproc.FONT_HERSHEY_SIMPLEX, .8, new Scalar(0, 0, 0), 2);
        Imgproc.putText(out, frame, new Point(0, 20), Imgproc.FONT_HERSHEY_SIMPLEX, .8, new Scalar(255, 255, 255), 1);

        if (ugui.user_info_text().length() > 0) // is there a message to display?
        {
            // if ( ! (ugui.user_info_text().equals("initialization"))) // stop spamming "initialization" to log
            // {
            // logger.log(Level.WARNING,ugui.user_info_text());
            // }
            String message1 = ugui.user_info_text();
            Imgproc.putText(out, message1, new Point(80, 20), Imgproc.FONT_HERSHEY_SIMPLEX, .8, new Scalar(0, 0, 0), 2);
            Imgproc.putText(out, message1, new Point(80, 20), Imgproc.FONT_HERSHEY_SIMPLEX, .8, new Scalar(255, 255, 255), 1);
        } 

        if (fewCorners)
        {
            String message2 = "moving or bad aim\n";
            Imgproc.putText(out, message2, new Point(80, 40), Imgproc.FONT_HERSHEY_SIMPLEX, .8, new Scalar(0, 0, 0), 2);
            Imgproc.putText(out, message2, new Point(80, 40), Imgproc.FONT_HERSHEY_SIMPLEX, .8, new Scalar(255, 255, 255), 1);
        }

        // ugui.tgt_r() ugui.tgt_t guidance board target rotation and translation from pose generation
        Mat rotationMatrix = new Mat();
        double[] rotationDegrees;
        double[] translation = new double[3];
        
        // if Guidance Board has a pose then display it (end of program is missing this pose)
        if ( ! ugui.tgt_r().empty() && ! ugui.tgt_t().empty())
        {
            Calib3d.Rodrigues(ugui.tgt_r(), rotationMatrix);
            rotationDegrees = Calib3d.RQDecomp3x3(rotationMatrix, new Mat(), new Mat()); // always returns reuler.length = 3
            rotationDegrees[0] -= 180.;

            ugui.tgt_t().get(0, 0, translation);

            Imgproc.putText(out, String.format("r{%4.0f %4.0f %4.0f} t{%4.0f %4.0f %4.0f}Guidance",
                rotationDegrees[0], rotationDegrees[1], rotationDegrees[2], translation[0], translation[1], translation[2]),
                new Point(250, 60), Imgproc.FONT_HERSHEY_SIMPLEX, .6, new Scalar(0, 0, 0), 2);
            Imgproc.putText(out, String.format("r{%4.0f %4.0f %4.0f} t{%4.0f %4.0f %4.0f}Guidance",
                rotationDegrees[0], rotationDegrees[1], rotationDegrees[2], translation[0], translation[1], translation[2]),
                new Point(250, 60), Imgproc.FONT_HERSHEY_SIMPLEX, .6, new Scalar(255, 255, 255), 1);
        }

        // if camera has a ChArUco Board pose then display it (if camera not on target this pose is missing)
        if ( ! ugui.tracker.rvec().empty() && ! ugui.tracker.tvec().empty())
        {
            Calib3d.Rodrigues(ugui.tracker.rvec(), rotationMatrix);
            rotationDegrees = Calib3d.RQDecomp3x3(rotationMatrix, new Mat(), new Mat()); // always returns reuler.length = 3
            rotationDegrees[1] = -rotationDegrees[1];
            rotationDegrees[2] = -rotationDegrees[2];

            ugui.tracker.tvec().get(0, 0, translation);
            // translation[1] = -translation[1];
            translation[0] = (double)(((int)(translation[0])+5)/10*10);
            translation[1] = (double)(((int)(translation[1])+5)/10*10);
            translation[2] = (double)(((int)(translation[2])+5)/10*10);
            Imgproc.putText(out, String.format("r{%4.0f %4.0f %4.0f} t{%4.0f %4.0f %4.0fCamera",
                rotationDegrees[0], rotationDegrees[1], rotationDegrees[2], translation[0], translation[1], translation[2]),
                new Point(250, 80), Imgproc.FONT_HERSHEY_SIMPLEX, .6, new Scalar(0, 0, 0), 2);
            Imgproc.putText(out, String.format("r{%4.0f %4.0f %4.0f} t{%4.0f %4.0f %4.0f}Camera",
                rotationDegrees[0], rotationDegrees[1], rotationDegrees[2], translation[0], translation[1], translation[2]),
                new Point(250, 80), Imgproc.FONT_HERSHEY_SIMPLEX, .6, new Scalar(255, 255, 255), 1);
        }

        // write a frame to a file name java<frame nbr>.jpg
        // final MatOfInt writeBoardParams = new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, 100); // debugging - pair-wise; param1, value1, ...
        // Imgcodecs.imwrite("java" + frame + ".jpg", out, writeBoardParams); // debugging - save image in jpg file
        
        if ( ! progressInsert.empty())
        {
            // add to the display the board/camera overlap image
            Imgproc.resize(progressInsert, progressInsert, new Size(this.img_size.width*0.1, this.img_size.height*0.1), 0, 0, Imgproc.INTER_CUBIC);
            List<Mat> temp1 = new ArrayList<>(3); // make the 1 b&w channel into 3 channels
            temp1.add(progressInsert);
            temp1.add(progressInsert);
            temp1.add(progressInsert);
            Mat temp2 = new Mat();
            Core.merge(temp1, temp2);
            Imgproc.rectangle(temp2, // outline the insert for better visibility
                new Point(0, 0),
                new Point(progressInsert.cols()-1., progressInsert.rows()-1.),
                new Scalar(255., 255., 0.), 1);
            temp2.copyTo(out.submat((int)(this.img_size.height*0.45), (int)(this.img_size.height*0.45)+progressInsert.rows(), 0,progressInsert.cols()));
            temp2.release();

            Imgproc.putText(out,
                String.format("similar%5.2f/%4.2f", ugui.pose_close_to_tgt_get(), Cfg.pose_close_to_tgt_min),
                new Point(0,(int)(this.img_size.height*0.45)+progressInsert.rows()+20) , Imgproc.FONT_HERSHEY_SIMPLEX, 0.6, new Scalar(255, 255, 255), 1);
        }  

        // display intrinsics convergence
        for (int i = 0; i < 9; i++)
        {
            Scalar color;
            if (ugui.pconverged()[i])
            {
                color = new Scalar(0, 190, 0);
            }
            else
            {
                color = new Scalar(0, 0, 255);
            }
            Imgproc.rectangle(out, new Point((double)i*20,this.img_size.height*0.4), new Point((double)(i+1)*20, this.img_size.height*0.4+20), color, Imgproc.FILLED);
            Imgproc.putText(out, ugui.INTRINSICS()[i],
                new Point((double)i*20, this.img_size.height*0.4+15),
                Imgproc.FONT_HERSHEY_SIMPLEX, .4, new Scalar(255, 255, 255), 1);
        }
    }

    public static class FindCornersGuidancePipeParams {
        final boolean save;
        final int boardHeight;
        final int boardWidth;
        final UICalibrationData.BoardType type;
        final double gridSize;
        final FrameDivisor divisor;

        public FindCornersGuidancePipeParams(
                boolean save,
                int boardHeight,
                int boardWidth,
                UICalibrationData.BoardType type,
                double gridSize,
                FrameDivisor divisor) {
            this.save = save;
            this.boardHeight = boardHeight;
            this.boardWidth = boardWidth;
            this.type = type;
            this.gridSize = gridSize; // mm
            this.divisor = divisor;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (save ? 1 : 0);
            result = prime * result + boardHeight;
            result = prime * result + boardWidth;
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            long temp;
            temp = Double.doubleToLongBits(gridSize);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            result = prime * result + ((divisor == null) ? 0 : divisor.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            FindCornersGuidancePipeParams other = (FindCornersGuidancePipeParams) obj;
            if (save != other.save) return false;
            if (boardHeight != other.boardHeight) return false;
            if (boardWidth != other.boardWidth) return false;
            if (type != other.type) return false;
            if (Double.doubleToLongBits(gridSize) != Double.doubleToLongBits(other.gridSize))
                return false;
            return divisor == other.divisor;
        }
    }

/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
/*                                                                                                 */
/*                                     FindBoardCornersGuidancePipeResult class                    */
/*                                     FindBoardCornersGuidancePipeResult class                    */
/*                                     FindBoardCornersGuidancePipeResult class                    */
/*                                                                                                 */
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
    public class FindBoardCornersGuidancePipeResult {
        public Size imgSize; // camera resolution or camera image size
        public Mat objCorners; // locations of detected undistorted object board corners
        public Mat imgCorners; // locations of detected perspective distorted board corners as seen by the camera image
        public Mat idCorners; // ids of the detected corners
        public boolean takeSnapshot; // good to capture this image for final calibration
        public boolean haveEnough; // converged and completed all guidance poses
        public boolean cancelCalibration; // image size error or other fatal error
    }
}
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
/*                                                                                                 */
/*                                     End FindBoardCornersGuidancePipe class                      */
/*                                     End FindBoardCornersGuidancePipe class                      */
/*                                     End FindBoardCornersGuidancePipe class                      */
/*                                                                                                 */
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
