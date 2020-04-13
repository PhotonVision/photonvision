package com.chameleonvision.common.vision.pipeline.impl;

import com.chameleonvision._2.config.CameraCalibrationConfig;
import com.chameleonvision._2.config.ConfigManager;

import com.chameleonvision._2.vision.VisionManager;
import com.chameleonvision._2.vision.camera.CameraCapture;
import com.chameleonvision._2.vision.pipeline.CVPipeline;
import com.chameleonvision._2.vision.pipeline.impl.DriverVisionPipeline;
import com.chameleonvision._2.vision.pipeline.impl.StandardCVPipelineSettings;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.wpi.cscore.VideoMode;
import edu.wpi.first.wpilibj.util.Units;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class Calibrate3dPipeline extends CVPipeline<DriverVisionPipeline.DriverPipelineResult, StandardCVPipelineSettings> {

    //Needs to be 4x11 if using standard Asymmetric DotBoard
    private int checkerboardSquaresHigh = 7;
    private int checkerboardSquaresWide = 7;

    private MatOfPoint3f objP;// new MatOfPoint3f(checkerboardSquaresHigh + checkerboardSquaresWide, 3);//(checkerboardSquaresWide * checkerboardSquaresHigh, 3);
    private Size patternSize = new Size(checkerboardSquaresHigh, checkerboardSquaresWide);
    private Size imageSize;
    double checkerboardSquareSize = 1; // inches!
    double grid_size = 15; //mm for dotboard

    private MatOfPoint2f calibrationOutput = new MatOfPoint2f();
    private List<Mat> objpoints = new ArrayList<>();
    private List<Mat> imgpoints = new ArrayList<>();

    private Mat stdDeviationsIntrinsics = new Mat();
    private Mat stdDeviationsExtrinsics = new Mat();
    private Mat perViewErrors = new Mat();

    public static double checkerboardSquareSizeUnits = Units.inchesToMeters(1.0);

    public static final int MIN_COUNT = 20;
    private VideoMode calibrationMode;
    private final Size windowSize = new Size(11, 11);
    private final Size zeroZone = new Size(-1, -1);
    private TermCriteria criteria = new TermCriteria(3, 30, 0.001); //(Imgproc.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 30, 0.001)

    private int captureCount = 0;
    private double calibrationAccuracy = 0;
    private boolean wantsSnapshot = false;
    private double squareSizeInches;



    public Calibrate3dPipeline(StandardCVPipelineSettings settings) {
        super(settings);

        objP = new MatOfPoint3f();

        if(settings.isUsingChessboard) {
            for (int i = 0; i < checkerboardSquaresHigh * checkerboardSquaresWide; i++) {
                objP.push_back(new MatOfPoint3f(new Point3(i / checkerboardSquaresWide, i % checkerboardSquaresHigh, 0.0f)));
            }
        }else{
            //Since this is an asymmetric dotboard, we cannot generate a NxN Mat since the board has different alternating dots each column (first column has 4 dots, 2nd has 5, 3rd has 4...)
            //From https://docs.opencv.org/master/d4/d94/tutorial_camera_calibration.html
            for( int i = 0; i < checkerboardSquaresHigh; i++ ) {
                for (int j = 0; j < checkerboardSquaresWide; j++) {
                    objP.push_back(new MatOfPoint3f( new Point3((2 * j + i % 2) * grid_size, i * grid_size, 0.0d)));
                }
            }
        }

        setSquareSize(checkerboardSquareSizeUnits);

        objpoints.forEach(Mat::release);
        imgpoints.forEach(Mat::release);
        objpoints.clear();
        imgpoints.clear();
    }

    public void setSquareSize(double size) {
        this.squareSizeInches = size;
    }

    public void takeSnapshot() {
        wantsSnapshot = true;
    }

    public boolean hasEnoughSnapshots() {
        return captureCount >= MIN_COUNT - 1;
    }

    //Remove a snapshot at given index and returns false if the index is out of bounds
    public boolean removeSnapShot(int index){
        if(index >= imgpoints.size()){return false;}
        imgpoints.remove(index);
        captureCount--;
        return true;
    }

    @Override
    public DriverVisionPipeline.DriverPipelineResult runPipeline(Mat inputMat) {

        // look for checkerboard
        Imgproc.cvtColor(inputMat, inputMat, Imgproc.COLOR_BGR2GRAY);
        boolean checkerboardFound;
        if(settings.isUsingChessboard) {
            checkerboardFound = Calib3d.findChessboardCorners(inputMat, patternSize, calibrationOutput);
        }else{
            checkerboardFound = Calib3d.findCirclesGrid(inputMat, patternSize, calibrationOutput, Calib3d.CALIB_CB_ASYMMETRIC_GRID);
        }



        if(!checkerboardFound) {
            Imgproc.cvtColor(inputMat, inputMat, Imgproc.COLOR_GRAY2BGR);

            return new DriverVisionPipeline.DriverPipelineResult(null, inputMat, 0);
        }

//        System.out.println("[SolvePNP] checkerboard found!!");

        // cool we found a checkerboard
        // do corner subpixel
        Imgproc.cornerSubPix(inputMat, calibrationOutput, windowSize, zeroZone, criteria);

        // convert back to BGR
        Imgproc.cvtColor(inputMat, inputMat, Imgproc.COLOR_GRAY2BGR);
        // draw the chessboard
        Calib3d.drawChessboardCorners(inputMat, patternSize, calibrationOutput, true);

        if(wantsSnapshot) {
            this.imageSize = new Size(inputMat.width(), inputMat.height());

            var mat = new MatOfPoint3f();
            calibrationOutput.copyTo(mat);
            this.objpoints.add(objP);
            imgpoints.add(mat);
            captureCount++;
            wantsSnapshot = false;
        }

        imageSize = new Size(inputMat.width(), inputMat.height());

        return new DriverVisionPipeline.DriverPipelineResult(null, inputMat, 0);
    }

    @Override
    public void initPipeline(CameraCapture camera) {
        super.initPipeline(camera);
        objpoints.clear();
        imgpoints.clear();
        captureCount = 0;
    }

    public boolean tryCalibration() {
        if (!hasEnoughSnapshots()) return false;

        Mat cameraMatrix = new Mat();
        Mat distortionCoeffs = new Mat();
        List<Mat> rvecs = new ArrayList<>();
        List<Mat> tvecs = new ArrayList<>();

        try {
            calibrationAccuracy = Calib3d.calibrateCameraExtended(objpoints, imgpoints, imageSize, cameraMatrix, distortionCoeffs, rvecs, tvecs, stdDeviationsIntrinsics, stdDeviationsExtrinsics, perViewErrors);
        } catch(Exception e) {
            e.printStackTrace();
            System.err.println("Camera calibration failed!");
            initPipeline(cameraCapture);
            return false;
        }

        VideoMode currentVidMode = cameraCapture.getCurrentVideoMode();
        Size resolution = new Size(currentVidMode.width, currentVidMode.height);
        CameraCalibrationConfig cal = new CameraCalibrationConfig(resolution, cameraMatrix, distortionCoeffs, squareSizeInches);

        VisionManager.getCurrentUIVisionProcess().addCalibration(cal);

        try {
            System.out.printf("CALIBRATION SUCCESS (with accuracy %s)! camMatrix: \n%s\ndistortionCoeffs:\n%s\n",
                    calibrationAccuracy, new ObjectMapper().writeValueAsString(cal.cameraMatrix), new ObjectMapper().writeValueAsString(cal.distortionCoeffs));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }


        ConfigManager.saveGeneralSettings();

        return true;
    }

    public void setVideoMode(VideoMode mode){
        this.calibrationMode = mode;
    }

    public int getSnapshotCount() {
        return captureCount + 1;
    }

    public double getCalibrationAccuracy(){
        return calibrationAccuracy;
    }

    public Mat getStdDeviationsIntrinsics() { return stdDeviationsIntrinsics;}

    public Mat getStdDeviationsExtrinsics() { return stdDeviationsExtrinsics; }

    public Mat getPerViewErrors() { return perViewErrors; }
}
