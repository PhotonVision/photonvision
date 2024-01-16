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

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.LogLevel;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.pipe.CVPipe;
import org.photonvision.vision.pipe.impl.FindBoardCornersPipe.FindCornersPipeParams;
import org.photonvision.vision.pipe.impl.Calibrate3dPoseGuidance.Cfg;
import org.photonvision.vision.pipe.impl.Calibrate3dPoseGuidance.ChArucoDetector;
import org.photonvision.vision.pipe.impl.Calibrate3dPoseGuidance.UserGuidance;

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
                FindBoardCornersPipe.FindCornersPipeParams> {

    private static final String VERSION = "beta 12.12"; // change this

    static final Logger logger = new Logger(FindBoardCornersGuidancePipe.class, LogGroup.General);

    //FIXME alll this "constructor" stuff has to be put into createGuidance becasue that's now the effective constructor
    private static PrintWriter vnlog = null;

    Mat progressInsert = new Mat();
    
    static Size img_size = new Size(1280, 720); //FIXME patch - info must come from frame
    static int image_width = 1280; //FIXME patch - info must come from first frame
    static int image_height = 720; //FIXME patch - info must come from frame

    ChArucoDetector tracker;

    UserGuidance ugui;

    // runtime variables
    boolean mirror = false;
    boolean save = false; // indicator for user pressed the "c" key to capture (save) manually
    String endMessage = "logic error"; // status of calibration at the end - if this initial value isn't reset, indicates a screw-up in the code

    int frameNumber = 0;

    FindBoardCornersGuidancePipeResult findBoardCornersGuidancePipeResult = new FindBoardCornersGuidancePipeResult();

    //FIXME need to add the size, object points and image points to the result when there is a capture

    private FindCornersPipeParams lastParams = null;

    public boolean createGuidance() { // first time through processing and verify resolution
        if (this.lastParams != null &&  ! this.lastParams.equals(this.params)) return false;// if params changed that's bad so cancel the calibration
        if (this.lastParams != null) return true; // leave if not the first time     

        this.lastParams = this.params; // set no longer first time

        //FIXME this.img_size = this.params.resolution; // nothing but a name change for the same data

        tracker = new ChArucoDetector(img_size);
        ugui = new UserGuidance(tracker, Cfg.var_terminate, img_size);
        return true;
    }


/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
/*                                                                                                 */
/*                                     FindBoardCornersGuidancePipe constructor                    */
/*                                     FindBoardCornersGuidancePipe constructor                    */
/*                                     FindBoardCornersGuidancePipe constructor                    */
/*                                                                                                 */
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
    public FindBoardCornersGuidancePipe()
    {

    }

    /**
     * Finds the corners in a given image and returns them
     *
     * @param in Input for pipe processing. Pair of input and output mat
     * @return All valid Mats for camera calibration
     */
    @Override
    protected FindBoardCornersGuidancePipeResult process(Pair<Mat, Mat> in) {
        if ( ! createGuidance()) { // first time through processing and verify resolution
            findBoardCornersGuidancePipeResult.takeSnapshot = false;
            findBoardCornersGuidancePipeResult.haveEnough = false;
            findBoardCornersGuidancePipeResult.cancelCalibration = true;
            return findBoardCornersGuidancePipeResult;
        }

        return findBoardCornersGuidance(in);
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
        Logger.setLevel(LogGroup.General, LogLevel.DEBUG);
        logger.info("Pose Guidance Camera Calibration version " + VERSION);

        // Path to save images
        final Path imageDir = ConfigManager.getInstance().getCalibDir();

        Mat imgPV = in.getLeft();
        Mat outPV = in.getRight();

        Mat img = new Mat();
        imgPV.copyTo(img);

        Size img_size = new Size(img.width(), img.height());
        Size img_size_start = img_size.clone();        
        Mat out = new Mat(); // user display Mat

        frameNumber++;
        if (frameNumber%Cfg.garbageCollectionFrames == 0) // periodically cleanup old Mats
        {
            System.runFinalization();
            System.gc();
        }
        boolean force = false;  // force add frame to calibration (no support yet still images else (force = !live)

        img_size = new Size(img.width(), img.height());
        // if image size changes during calibration bail out and get ready to calibrate again with new size
        if ( ! img_size_start.equals(img_size))               
        {
            logger.warn("changing image size to " + img_size + " from " + img_size_start + ", canceling calibration");
            // quit this calibration and start anew
            findBoardCornersGuidancePipeResult.takeSnapshot = false;
            findBoardCornersGuidancePipeResult.haveEnough = false;
            findBoardCornersGuidancePipeResult.cancelCalibration = true;
            return findBoardCornersGuidancePipeResult;
        }

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
            //FIXME return skip this image
        }

        boolean capturedPose = ugui.update(force, progressInsert); // calibrate
        
        if (capturedPose /*&& Cfg.logDetectedCorners*/)
        {
            endMessage = "CAPTURED";
            logDetectedCorners(img, ugui);
        }

        displayOverlay(out, ugui, fewCorners, frameNumber, progressInsert);

        out.copyTo(outPV);

        if (ugui.converged()) // are we there yet?
        {
            ugui.calib.calibrate(new ArrayList<>(1)); // final, maybe more accurate calibration; dummy arg to use all captures

            ugui.write(); // write all the calibration data

            endMessage = "CALIBRATED";
        }

        Imgproc.putText(out, endMessage, new Point(50, 250), Imgproc.FONT_HERSHEY_SIMPLEX, 2.8, new Scalar(0, 0, 0), 5);
        Imgproc.putText(out, endMessage, new Point(50, 250), Imgproc.FONT_HERSHEY_SIMPLEX, 2.8, new Scalar(255, 255, 255), 3);
        Imgproc.putText(out, endMessage, new Point(50, 250), Imgproc.FONT_HERSHEY_SIMPLEX, 2.8, new Scalar(255, 255, 0), 2);

        out.copyTo(outPV);

        if (vnlog != null)
        {
            vnlog.close(); // user has to grab this file before the next capture otherwise it's gone and starting over
            vnlog = null;
        }

// DONE HERE
// WHAT DOES PV WANT?

// findBoardCornersPipe.findBoardCorners returns the corners detected in the image
// I assume it is run when the take snapshot button is pressed
//  and loops repeatedly until the done calibration button is pressed
// then this happens:
// Calibrate3dPipe has the calibrateCameraExtended with this comment:
// first it has these parameters
// CameraCalibrationCoefficients process(List<Triple<Size, Mat, Mat>> in) 
// so I need to make a list of the Triple (keyframes)
// then call the calibratecameraExtended
// FindBoardCorners pipe outputs all the image points, object points, and frames to calculate
// imageSize from, other parameters are output Mats

// thus all this autocalibration stuff has all the data at this point and it's time to run
// the final calibration in PV

// click button to end calibration goes to requestHandler.java onCalibrationEndRequestonCalibrationEndRequest()
//goes to VisionModule.java endCalibration()
// which runs         var ret = pipelineManager.calibration3dPipeline.tryCalibration();
// which returns the ret type CameraCalibrationCoefficients

// when we like the frame could tap into PV here:
// public void takeSnapshot() {
//     takeSnapshot = true;
// }
// isCalibrating ? "Take Snapshot" : "Start Calibration" 
// at end "Finish Calibration"
        // most frames will return falses
        findBoardCornersGuidancePipeResult.takeSnapshot = false;
        findBoardCornersGuidancePipeResult.haveEnough = false;
        findBoardCornersGuidancePipeResult.cancelCalibration = false;

        if (endMessage.equals("CALIBRATED"))
        {
            findBoardCornersGuidancePipeResult.takeSnapshot = true;
            findBoardCornersGuidancePipeResult.haveEnough = true;
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
        }

        //FIXME copy the obj points and img points to the result return

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
    public static void displayOverlay(Mat out, UserGuidance ugui, boolean fewCorners, int frameNumber, Mat progressInsert)
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
            Imgproc.resize(progressInsert, progressInsert, new Size(image_width*0.1, image_height*0.1), 0, 0, Imgproc.INTER_CUBIC);
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
            temp2.copyTo(out.submat((int)(image_height*0.45), (int)(image_height*0.45)+progressInsert.rows(), 0,progressInsert.cols()));
            temp2.release();

            Imgproc.putText(out,
                String.format("similar%5.2f/%4.2f", ugui.pose_close_to_tgt_get(), Cfg.pose_close_to_tgt_min),
                new Point(0,(int)(image_height*0.45)+progressInsert.rows()+20) , Imgproc.FONT_HERSHEY_SIMPLEX, 0.6, new Scalar(255, 255, 255), 1);
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
            Imgproc.rectangle(out, new Point((double)i*20,image_height*0.4), new Point((double)(i+1)*20, image_height*0.4+20), color, Imgproc.FILLED);
            Imgproc.putText(out, ugui.INTRINSICS()[i],
                new Point((double)i*20, image_height*0.4+15),
                Imgproc.FONT_HERSHEY_SIMPLEX, .4, new Scalar(255, 255, 255), 1);
        }
    }
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
/*                                                                                                 */
/*                                     logDetectedCorners                                          */
/*                                     logDetectedCorners                                          */
/*                                     logDetectedCorners                                          */
/*                                                                                                 */
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
    /**
     * Detected Board data in mrgingham format plus the board info
     * @param img Camera image
     * @param ugui User Guidance Class
     * @throws FileNotFoundException
     */
    public static void logDetectedCorners(Mat img, UserGuidance ugui)// throws FileNotFoundException
    {
        if (vnlog == null) // first time switch
        {
            try {
                vnlog = new PrintWriter(Cfg.cornersLog);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            vnlog.println("## produced by pose guidance calibration program");
            vnlog.println("# filename x y level cid boardX boardY");
        }

        // write the captured frame to a file name
        int captureCount = ugui.calib.keyframes.size();

        String filename = String.format("img%02d.jpg", captureCount);
        final MatOfInt writeBoardParams = new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, 100); // pair-wise; param1, value1, ...
        Imgcodecs.imwrite(filename, img, writeBoardParams); // save camera image

        // for efficiency put Mat data in arrays
        Point3[] ChessboardCorners = ugui.tracker.board().getChessboardCorners().toArray(); // 1 col 3 channels x, y, z in a Point3 (float)

        int[] DetectedCIDs = new int[ugui.tracker.cids().rows()]; // get detected corners - assume captured image does have corners
        ugui.tracker.cids().get(0, 0, DetectedCIDs);

        float[] DetectedCorners = new float[ugui.tracker.ccorners().rows()*ugui.tracker.ccorners().cols()*ugui.tracker.ccorners().channels()]; // below assumes x and y in a row
        ugui.tracker.ccorners().get(0, 0, DetectedCorners);

        // save vnlog     
        for (int detectedCornerIndex = 0; detectedCornerIndex < DetectedCIDs.length; detectedCornerIndex++)
        {
            int boardCornerId = DetectedCIDs[detectedCornerIndex]; // get board corner that is detected
            StringBuilder logLine = new StringBuilder();
            logLine.append(filename);
            logLine.append(" ");
            logLine.append(DetectedCorners[detectedCornerIndex*2]); // x
            logLine.append(" ");
            logLine.append(DetectedCorners[detectedCornerIndex*2+1]); // y
            logLine.append(" 0 "); // intended to be decimations always 0
            logLine.append(boardCornerId);
            logLine.append(" ");
            logLine.append(ChessboardCorners[boardCornerId].x); // x
            logLine.append(" ");
            logLine.append(ChessboardCorners[boardCornerId].y); // y
            vnlog.println(logLine.toString());                    
        }
    }

    public class FindBoardCornersGuidancePipeResult {
        public Size imgSize; // camera resolution or camera image size
        public MatOfPoint3f objCorners; // locations of undistorted board corners
        public MatOfPoint2f imgCorners; // locations of detected perspective distorted board corners as seen by the camera
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

//  https://github.com/mcm001/photonvision/tree/2023-10-30_pose_calib_integration
//  I made this by running 
// gradlew clean
//  then for RPi
// gradlew shadowjar -PArchOverride=linuxarm64
//  or for Windows
// gradlew shadowjar

//  inside the photonvision project's root directory
//  that spits the jar out into photon-server/build/libs
//  you should be able to stop the RPi photonvision service with 
// sudo service photonvision stop
//  and then 
// java -jar photonvision-dev-v2024.1.1-beta-3.1-5-ga99e85a8-linuxarm64.jar
//  is all you should need

// Disable spotless in VSCode extensions or Append "-x spotlessapply" to the commands you run to disable it
