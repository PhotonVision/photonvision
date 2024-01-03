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

package org.photonvision.calibrator;

import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.cscore.CvSink;
import edu.wpi.first.cscore.CvSource;
import edu.wpi.first.cscore.MjpegServer;
import edu.wpi.first.cscore.UsbCamera;
import edu.wpi.first.cscore.VideoMode;
import edu.wpi.first.cscore.VideoMode.PixelFormat;
import edu.wpi.first.cscore.VideoProperty;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.LogLevel;
import org.photonvision.common.logging.Logger;

/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
/*                                                                                                 */
/*                                     Main class                                                  */
/*                                     Main class                                                  */
/*                                     Main class                                                  */
/*                                                                                                 */
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
public class Main {
    private static final String VERSION = "beta 12.7"; // change this
    static final Logger logger = new Logger(Main.class, LogGroup.General);

    private static int frameNumber = 0;
    static String frame = "00000 ";
    private static PrintWriter vnlog = null;
    static Mat progressInsert;
    static boolean fewCorners = true;
    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    /*                                                                                                 */
    /*                                     handle keystrokes                                           */
    /*                                     handle keystrokes                                           */
    /*                                     handle keystrokes                                           */
    /*                                                                                                 */
    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    // keyboard mapping returns from OpenCV waitKey
    private static final int keyTerminateOpenCV = 81;
    private static final int keyCaptureOpenCV = 67;
    private static final int keyMirrorToggleOpenCV = 77;

    // keyboard mapping returns from Java Scanner
    private static final int keyTerminateScanner = 113;
    private static final int keyCaptureScanner = 99;
    private static final int keyMirrorToggleScanner = 109;
    private static final int timedOut = -1; // timed out no key pressed

    // Java Scanner alternative to OpenCV keyboard usage that is not in PV headless (AWT is missing)
    // Turn Scanner keys into OpenCV keys to ease transition back and forth between PV terminal and
    // OpenCV waitKey
    AtomicInteger dokeystroke = new AtomicInteger(-1);

    class Keystroke implements Runnable {
        final Logger logger = new Logger(Main.Keystroke.class, LogGroup.General);

        public void run() {
            try (Scanner keyboard = new Scanner(System.in)) {
                while (!Thread.interrupted()) {
                    System.out.println(
                            "Pose should auto capture otherwise, press c (capture), m (mirror), q (quit) then the Enter key");
                    String entered = keyboard.next();
                    int keyScanner = entered.charAt(0);
                    logger.debug("user entered action " + keyScanner);
                    // map Scanner character codes to OpenCV character codes
                    if (keyScanner == keyCaptureScanner) {
                        dokeystroke.set(keyCaptureOpenCV);
                    }
                    if (keyScanner == keyMirrorToggleScanner) {
                        dokeystroke.set(keyMirrorToggleOpenCV);
                    }
                    if (keyScanner == keyTerminateScanner) {
                        dokeystroke.set(keyTerminateOpenCV);
                    }
                }
            } catch (Exception e) {
                logger.error(
                        "Terminal keyboard closed prematurely (Ctrl-c) or doesn't exist (jar file not run from command line; don't double click the jar to start it)");
            }
        }
    }

    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    /*                                                                                                 */
    /*                                     handleArgs                                                  */
    /*                                     handleArgs                                                  */
    /*                                     handleArgs                                                  */
    /*                                                                                                 */
    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    private static boolean handleArgs(String[] args) throws ParseException {
        final var options = new Options();
        options.addOption("h", "help", false, "Show this help text and exit");
        options.addOption("width", true, "camera image width (1280)");
        options.addOption("height", true, "camera image height (720)");
        options.addOption("dpmX", true, "print width pixels per meter (9843=250 DPI)");
        options.addOption("dpmY", true, "print height pixels per meter (9843=250 DPI");
        options.addOption(
                "pxFmt", true, "pixel format (kYUYV) " + Arrays.toString(PixelFormat.values()));
        options.addOption("cameraId", true, "camera id (0)");
        options.addOption("fps", true, "frames per second (10)");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar <your jar file>.jar [options]", options);
            return false; // exit program
        }

        if (cmd.hasOption("width")) {
            Cfg.image_width = Integer.parseInt(cmd.getOptionValue("width"));
        }

        if (cmd.hasOption("height")) {
            Cfg.image_height = Integer.parseInt(cmd.getOptionValue("height"));
        }

        if (cmd.hasOption("dpmX")) {
            Cfg.resXDPM = Integer.parseInt(cmd.getOptionValue("dpmX"));
        }

        if (cmd.hasOption("dpmY")) {
            Cfg.resYDPM = Integer.parseInt(cmd.getOptionValue("dpmY"));
        }

        if (cmd.hasOption("pxFmt")) {
            Cfg.pixelFormat = PixelFormat.valueOf(cmd.getOptionValue("pxFmt"));
        }

        if (cmd.hasOption("cameraId")) {
            Cfg.camId = Integer.parseInt(cmd.getOptionValue("cameraId"));
        }

        if (cmd.hasOption("fps")) {
            Cfg.fps = Integer.parseInt(cmd.getOptionValue("fps"));
        }

        return true;
    }

    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    /*                                                                                                 */
    /*                                     main                                                        */
    /*                                     main                                                        */
    /*                                     main                                                        */
    /*                                                                                                 */
    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    public static void main(String[] args) throws Exception {
        Logger.setLevel(LogGroup.General, LogLevel.DEBUG);
        logger.info("Pose Guidance Camera Calibration version " + VERSION);
        // get the parameters for the user provided options
        logger.debug("Command Line Args " + Arrays.toString(args));

        try {
            if (!handleArgs(args)) {
                System.exit(0);
            }
        } catch (ParseException e) {
            logger.error("Failed to parse command-line options!", e);
            System.exit(0);
        }

        org.photonvision.common.util.TestUtils.loadLibraries();

        progressInsert = new Mat();

        // keyboard handler for PV environment using the web interface
        Main MainInstance = null;
        Keystroke keystroke;
        Thread keyboardThread;
        MainInstance = new Main();
        keystroke = MainInstance.new Keystroke();
        keyboardThread = new Thread(keystroke, "keys");
        keyboardThread.setDaemon(true);
        keyboardThread.start();

        // output display of the camera image with guidance board
        CvSource networkDisplay = null;
        MjpegServer mjpegServer;
        networkDisplay =
                new CvSource(
                        "calibPV", /*VideoMode.*/
                        PixelFormat.kMJPEG,
                        Cfg.image_height,
                        Cfg.image_width,
                        Cfg.fps);
        mjpegServer = new MjpegServer("GuidanceView", Cfg.displayPort);
        logger.info("View Guidance Board On Port " + Cfg.displayPort);
        mjpegServer.setSource(networkDisplay);
        Mat out = new Mat(); // user display Mat

        // camera input
        final UsbCamera camera =
                CameraServer.startAutomaticCapture(
                        Cfg.camId); // gives access to camera parameters on port 181 or above
        // final UsbCamera camera = new UsbCamera("mycamera", Cfg.camId); // same camera as above but no
        // interaction on port 181 or above
        for (VideoMode vm : camera.enumerateVideoModes()) {
            logger.debug(
                    "Camera mode choices "
                            + vm.getPixelFormatFromInt(vm.pixelFormat.getValue())
                            + " "
                            + vm.width
                            + "x"
                            + vm.height
                            + " "
                            + vm.fps
                            + " fps");
        }
        for (VideoProperty vp : camera.enumerateProperties()) {
            logger.debug(
                    "camera property choices "
                            + vp.get()
                            + " "
                            + vp.getName()
                            + " "
                            + VideoProperty.getKindFromInt(vp.get()));
        }
        VideoMode videoMode =
                new VideoMode(Cfg.pixelFormat, Cfg.image_width, Cfg.image_height, Cfg.fps);
        logger.debug(
                "Setting camera mode "
                        + VideoMode.getPixelFormatFromInt(Cfg.pixelFormat.getValue())
                        + " "
                        + Cfg.image_width
                        + "x"
                        + Cfg.image_height
                        + " "
                        + Cfg.fps
                        + "fps");
        try {
            if (!camera.setVideoMode(videoMode))
                throw new IllegalArgumentException("set video mode returned false");
        } catch (Exception e) {
            logger.error("camera set video mode error; mode is unchanged", e);
        }
        logger.info(
                "camera " + Cfg.camId + " properties can be seen and changed on port 1181 or higher");
        CvSink cap =
                CameraServer.getVideo(camera); // Get a CvSink. This will capture Mats from the camera
        cap.setSource(camera);
        Mat _img = new Mat(); // this follows the camera input but ...
        Mat img =
                new Mat(
                        Cfg.image_height,
                        Cfg.image_width,
                        CvType.CV_8UC3); // set by user config - need camera to return this size, too

        ChArucoDetector tracker = new ChArucoDetector();
        UserGuidance ugui = new UserGuidance(tracker, Cfg.var_terminate);

        // runtime variables
        boolean mirror = false;
        boolean save = false; // indicator for user pressed the "c" key to capture (save) manually
        String endMessage =
                "logic error"; // status of calibration at the end - if this initial value isn't reset,
        // indicates a screw-up in the code

        grabFrameLoop:
        while (!Thread.interrupted()) {
            frameNumber++;
            frame = String.format("%05d ", frameNumber);
            if (frameNumber % Cfg.garbageCollectionFrames == 0) // periodically cleanup old Mats
            {
                System.runFinalization();
                System.gc();
            }
            boolean force =
                    false; // force add frame to calibration (no support yet still images else (force = !live)

            long status = cap.grabFrame(_img, 0.5);
            if (status != 0) {
                if (_img.height() != Cfg.image_height
                        || _img.width()
                                != Cfg
                                        .image_width) // enforce camera matches user spec for testing and no good camera
                // setup
                {
                    logger.warn(
                            "camera size incorrect "
                                    + _img.width()
                                    + "x"
                                    + _img.height()
                                    + "; user specified to calibrate at "
                                    + Cfg.image_width
                                    + "x"
                                    + Cfg.image_height
                                    + " - ignoring it");
                    continue;
                }
                _img.copyTo(img);
            } else {
                logger.warn("grabFrame error " + cap.getError());
                force = false; // useless now with the continue below
                continue; // pretend frame never happened - rkt addition; original reprocessed previous
                // frame
            }

            tracker.detect(img);

            if (save) {
                save = false;
                force = true;
            }

            img.copyTo(out); // out has the camera image at his point

            ugui.draw(
                    out,
                    mirror); // this adds the guidance board to the camera image (out) to make the new out

            boolean capturedPose = ugui.update(force); // calibrate

            if (capturedPose && Cfg.logDetectedCorners) {
                logDetectedCorners(img, ugui);
            }

            displayOverlay(out, ugui);

            int k;
            networkDisplay.putFrame(out);
            k = MainInstance.dokeystroke.getAndSet(timedOut);

            if (ugui.converged()) // are we there yet?
            {
                ugui.write(); // write all the calibration data

                endMessage = "CALIBRATED";

                break grabFrameLoop; // the end - rkt addition; the original kept looping somehow
            }

            if (k == timedOut) {
                continue; // no key press to process
            }

            // have a key
            switch (k) {
                case keyTerminateOpenCV: // terminate key pressed to stop loop immediately
                    endMessage = "CANCELLED";
                    break grabFrameLoop;

                case keyMirrorToggleOpenCV: // mirror/no mirror key pressed
                    mirror = !mirror;
                    break;

                case keyCaptureOpenCV: // capture frame key pressed
                    save = true;
                    break;

                default: // unassigned key
                    break;
            }
        } // end grabFrameLoop

        Imgproc.putText(
                out,
                endMessage,
                new Point(50, 250),
                Imgproc.FONT_HERSHEY_SIMPLEX,
                2.8,
                new Scalar(0, 0, 0),
                5);
        Imgproc.putText(
                out,
                endMessage,
                new Point(50, 250),
                Imgproc.FONT_HERSHEY_SIMPLEX,
                2.8,
                new Scalar(255, 255, 255),
                3);
        Imgproc.putText(
                out,
                endMessage,
                new Point(50, 250),
                Imgproc.FONT_HERSHEY_SIMPLEX,
                2.8,
                new Scalar(255, 255, 0),
                2);

        for (int runOut = 0;
                runOut < 10;
                runOut++) // last frame won't display so repeat it a bunch of times to see it; q lags these
        // 2 seconds
        {
            networkDisplay.putFrame(out);
            Thread.sleep(200L);
        }
        networkDisplay.close();

        if (vnlog != null) {
            vnlog.close();
        }
        logger.debug("End of running main");
        System.exit(0);
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
    public static void displayOverlay(Mat out, UserGuidance ugui) {
        Imgproc.putText(
                out,
                Main.frame,
                new Point(0, 20),
                Imgproc.FONT_HERSHEY_SIMPLEX,
                .8,
                new Scalar(0, 0, 0),
                2);
        Imgproc.putText(
                out,
                Main.frame,
                new Point(0, 20),
                Imgproc.FONT_HERSHEY_SIMPLEX,
                .8,
                new Scalar(255, 255, 255),
                1);

        if (ugui.user_info_text().length() > 0) // is there a message to display?
        {
            // if ( ! (ugui.user_info_text().equals("initialization"))) // stop spamming "initialization"
            // to log
            // {
            // logger.log(Level.WARNING,ugui.user_info_text());
            // }
            String message1 = ugui.user_info_text();
            Imgproc.putText(
                    out,
                    message1,
                    new Point(80, 20),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    .8,
                    new Scalar(0, 0, 0),
                    2);
            Imgproc.putText(
                    out,
                    message1,
                    new Point(80, 20),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    .8,
                    new Scalar(255, 255, 255),
                    1);
        }

        if (fewCorners) {
            String message2 = "moving or bad aim\n";
            Imgproc.putText(
                    out,
                    message2,
                    new Point(80, 40),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    .8,
                    new Scalar(0, 0, 0),
                    2);
            Imgproc.putText(
                    out,
                    message2,
                    new Point(80, 40),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    .8,
                    new Scalar(255, 255, 255),
                    1);
            fewCorners = false;
        }

        // ugui.tgt_r() ugui.tgt_t guidance board target rotation and translation from pose generation
        Mat rotationMatrix = new Mat();
        double[] rotationDegrees;
        double[] translation = new double[3];

        // if Guidance Board has a pose then display it (end of program is missing this pose)
        if (!ugui.tgt_r().empty() && !ugui.tgt_t().empty()) {
            Calib3d.Rodrigues(ugui.tgt_r(), rotationMatrix);
            rotationDegrees =
                    Calib3d.RQDecomp3x3(
                            rotationMatrix, new Mat(), new Mat()); // always returns reuler.length = 3
            rotationDegrees[0] -= 180.;

            ugui.tgt_t().get(0, 0, translation);

            Imgproc.putText(
                    out,
                    String.format(
                            "r{%4.0f %4.0f %4.0f} t{%4.0f %4.0f %4.0f}Guidance",
                            rotationDegrees[0],
                            rotationDegrees[1],
                            rotationDegrees[2],
                            translation[0],
                            translation[1],
                            translation[2]),
                    new Point(250, 60),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    .6,
                    new Scalar(0, 0, 0),
                    2);
            Imgproc.putText(
                    out,
                    String.format(
                            "r{%4.0f %4.0f %4.0f} t{%4.0f %4.0f %4.0f}Guidance",
                            rotationDegrees[0],
                            rotationDegrees[1],
                            rotationDegrees[2],
                            translation[0],
                            translation[1],
                            translation[2]),
                    new Point(250, 60),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    .6,
                    new Scalar(255, 255, 255),
                    1);
        }

        // if camera has a ChArUco Board pose then display it (if camera not on target this pose is
        // missing)
        if (!ugui.tracker.rvec().empty() && !ugui.tracker.tvec().empty()) {
            Calib3d.Rodrigues(ugui.tracker.rvec(), rotationMatrix);
            rotationDegrees =
                    Calib3d.RQDecomp3x3(
                            rotationMatrix, new Mat(), new Mat()); // always returns reuler.length = 3
            rotationDegrees[1] = -rotationDegrees[1];
            rotationDegrees[2] = -rotationDegrees[2];

            ugui.tracker.tvec().get(0, 0, translation);
            // translation[1] = -translation[1];
            translation[0] = (double) (((int) (translation[0]) + 5) / 10 * 10);
            translation[1] = (double) (((int) (translation[1]) + 5) / 10 * 10);
            translation[2] = (double) (((int) (translation[2]) + 5) / 10 * 10);
            Imgproc.putText(
                    out,
                    String.format(
                            "r{%4.0f %4.0f %4.0f} t{%4.0f %4.0f %4.0fCamera",
                            rotationDegrees[0],
                            rotationDegrees[1],
                            rotationDegrees[2],
                            translation[0],
                            translation[1],
                            translation[2]),
                    new Point(250, 80),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    .6,
                    new Scalar(0, 0, 0),
                    2);
            Imgproc.putText(
                    out,
                    String.format(
                            "r{%4.0f %4.0f %4.0f} t{%4.0f %4.0f %4.0f}Camera",
                            rotationDegrees[0],
                            rotationDegrees[1],
                            rotationDegrees[2],
                            translation[0],
                            translation[1],
                            translation[2]),
                    new Point(250, 80),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    .6,
                    new Scalar(255, 255, 255),
                    1);
        }

        // write a frame to a file name java<frame nbr>.jpg
        // final MatOfInt writeBoardParams = new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, 100); //
        // debugging - pair-wise; param1, value1, ...
        // Imgcodecs.imwrite("java" + frame + ".jpg", out, writeBoardParams); // debugging - save image
        // in jpg file

        if (!progressInsert.empty()) {
            // add to the display the board/camera overlap image
            Imgproc.resize(
                    progressInsert,
                    progressInsert,
                    new Size(Cfg.image_width * 0.1, Cfg.image_height * 0.1),
                    0,
                    0,
                    Imgproc.INTER_CUBIC);
            List<Mat> temp1 = new ArrayList<>(3); // make the 1 b&w channel into 3 channels
            temp1.add(progressInsert);
            temp1.add(progressInsert);
            temp1.add(progressInsert);
            Mat temp2 = new Mat();
            Core.merge(temp1, temp2);
            Imgproc.rectangle(
                    temp2, // outline the insert for better visibility
                    new Point(0, 0),
                    new Point(progressInsert.cols() - 1., progressInsert.rows() - 1.),
                    new Scalar(255., 255., 0.),
                    1);
            temp2.copyTo(
                    out.submat(
                            (int) (Cfg.image_height * 0.45),
                            (int) (Cfg.image_height * 0.45) + progressInsert.rows(),
                            0,
                            progressInsert.cols()));
            temp2.release();

            Imgproc.putText(
                    out,
                    String.format(
                            "similar%5.2f/%4.2f", ugui.pose_close_to_tgt_get(), Cfg.pose_close_to_tgt_min),
                    new Point(0, (int) (Cfg.image_height * 0.45) + progressInsert.rows() + 20),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    0.6,
                    new Scalar(255, 255, 255),
                    1);
        }

        // display intrinsics convergence
        for (int i = 0; i < 9; i++) {
            Scalar color;
            if (ugui.pconverged()[i]) {
                color = new Scalar(0, 190, 0);
            } else {
                color = new Scalar(0, 0, 255);
            }
            Imgproc.rectangle(
                    out,
                    new Point((double) i * 20, Cfg.image_height * 0.4),
                    new Point((double) (i + 1) * 20, Cfg.image_height * 0.4 + 20),
                    color,
                    Imgproc.FILLED);
            Imgproc.putText(
                    out,
                    ugui.INTRINSICS()[i],
                    new Point((double) i * 20, Cfg.image_height * 0.4 + 15),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    .4,
                    new Scalar(255, 255, 255),
                    1);
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
     *
     * @param img Camera image
     * @param ugui User Guidance Class
     * @throws FileNotFoundException
     */
    public static void logDetectedCorners(Mat img, UserGuidance ugui) throws FileNotFoundException {
        if (vnlog == null) // first time switch
        {
            vnlog = new PrintWriter(Cfg.cornersLog);
            vnlog.println("## produced by pose guidance calibration program");
            vnlog.println("# filename x y level cid boardX boardY");
        }

        // write the captured frame to a file name
        int captureCount = ugui.calib.keyframes.size();

        String filename = String.format("img%02d.jpg", captureCount);
        final MatOfInt writeBoardParams =
                new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, 100); // pair-wise; param1, value1, ...
        Imgcodecs.imwrite(filename, img, writeBoardParams); // save camera image

        // for efficiency put Mat data in arrays
        Point3[] ChessboardCorners =
                ugui.tracker
                        .board()
                        .getChessboardCorners()
                        .toArray(); // 1 col 3 channels x, y, z in a Point3 (float)

        int[] DetectedCIDs =
                new int
                        [ugui.tracker
                                .cids()
                                .rows()]; // get detected corners - assume captured image does have corners
        ugui.tracker.cids().get(0, 0, DetectedCIDs);

        float[] DetectedCorners =
                new float
                        [ugui.tracker.ccorners().rows()
                                * ugui.tracker.ccorners().cols()
                                * ugui.tracker.ccorners().channels()]; // below assumes x and y in a row
        ugui.tracker.ccorners().get(0, 0, DetectedCorners);

        // save vnlog
        for (int detectedCornerIndex = 0;
                detectedCornerIndex < DetectedCIDs.length;
                detectedCornerIndex++) {
            int boardCornerId = DetectedCIDs[detectedCornerIndex]; // get board corner that is detected
            StringBuilder logLine = new StringBuilder();
            logLine.append(filename);
            logLine.append(" ");
            logLine.append(DetectedCorners[detectedCornerIndex * 2]); // x
            logLine.append(" ");
            logLine.append(DetectedCorners[detectedCornerIndex * 2 + 1]); // y
            logLine.append(" 0 "); // intended to be decimations always 0
            logLine.append(boardCornerId);
            logLine.append(" ");
            logLine.append(ChessboardCorners[boardCornerId].x); // x
            logLine.append(" ");
            logLine.append(ChessboardCorners[boardCornerId].y); // y
            vnlog.println(logLine.toString());
        }
    }
}
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
/*                                                                                                 */
/*                                     End Main class                                              */
/*                                     End Main class                                              */
/*                                     End Main class                                              */
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

// Disable spotless in VSCode extensions or Append "-x spotlessapply" to the commands you run to
// disable it
