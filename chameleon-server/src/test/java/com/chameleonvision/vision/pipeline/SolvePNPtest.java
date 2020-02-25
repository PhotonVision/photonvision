package com.chameleonvision.vision.pipeline;

import com.chameleonvision.config.CameraCalibrationConfig;
import com.chameleonvision.config.JsonMat;
import com.chameleonvision.vision.image.StaticImageCapture;
import com.chameleonvision.vision.pipeline.impl.StandardCVPipeline;
import com.chameleonvision.vision.pipeline.impl.StandardCVPipelineSettings;
import com.chameleonvision.vision.pipeline.pipes.SolvePNPPipe;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.wpi.cscore.CameraServerCvJNI;
import edu.wpi.cscore.CameraServerJNI;
import edu.wpi.first.wpilibj.geometry.Rotation2d;
import org.junit.jupiter.api.Test;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.nio.file.Path;

public class SolvePNPtest {

    private static final Path root = Path.of("src", "test", "java", "com", "chameleonvision", "vision", "pipeline");

    @Test public void testProjection() throws InterruptedException {

        try {
            forceLoad();
        } catch (IOException e) {
            return;
        }

        var settings = new StandardCVPipelineSettings();
        var calibration = new CameraCalibrationConfig(
                new Size(640, 480),
                new JsonMat(3, 3, 6, new double[] { 1126.1154452525066, 0.0, 666.4172679761178, 0.0, 1088.0425532065287, 335.37748454259633, 0.0, 0.0, 1.0 }),
                new JsonMat(1, 5, 6, new double[] { 0.07253724845871252, -0.664268685338307, -0.0011224914177033868, 4.8323234488098423E-4, 1.1731498589436031 }),
                1.056
                );

        var pipe = new SolvePNPPipe(settings, calibration, new Rotation2d());

        // project pts
        var target = settings.targetCornerMat;
        Mat blank = Imgcodecs.imread(Path.of(root.toString(), "black.png").toString());

        // make a tvec and rvec
//        positive x to the right, positive y to the bottom, positive z away from the image
        // tvec example:
//        [-3.377348429632199;
//        9.132802434424915;
//        67.79662519667924]

        // rvec:
//        [1.990867004634147;
//        -0.1508389335122144;
//        -1.552061845576413]

        Mat tvec = new Mat(3, 1, 6);
        Mat rvec = new Mat(3, 1, 6);
//        tvec.put(0, 0, 1.032188152287021,  -3.78145690753876, 52.32713732614368);
//        rvec.put(0, 0, -3.084531365719034, -0.1446574541579896,  -0.1297813889017779);
        tvec.put(0, 0, 1.75, -6, 75.2);
        rvec.put(0, 0, 2.79, 0.23, -0.0388);

        MatOfPoint2f imagePoints = new MatOfPoint2f();
        Calib3d.projectPoints(target, rvec, tvec, calibration.getCameraMatrixAsMat(), calibration.getDistortionCoeffsAsMat(), imagePoints, new Mat(), 0);
        var projectedPts = imagePoints.toList();

        // draw circles
        for(var p: projectedPts) {
            Imgproc.circle(blank, p, 3, new Scalar(0, 0, 255), 4);
        }
        Imgproc.line(blank, projectedPts.get(0), projectedPts.get(1), new Scalar(255, 0, 0));
        Imgproc.line(blank, projectedPts.get(1), projectedPts.get(2), new Scalar(255, 0, 0));
        Imgproc.line(blank, projectedPts.get(2), projectedPts.get(3), new Scalar(255, 0, 0));
        Imgproc.line(blank, projectedPts.get(3), projectedPts.get(0), new Scalar(255, 0, 0));

        // go backwards to solvePNP
        Mat rvec_ = new Mat(), tvec_ = new Mat();
        Calib3d.solvePnP(target, imagePoints, calibration.getCameraMatrixAsMat(), calibration.getDistortionCoeffsAsMat(), rvec_, tvec_);

        // what we projected
        var initTvec = tvec.dump();
        var initRvec = rvec.dump();

        // what solvePNP gives
        var retedTvec = tvec_.dump();
        var rettedRvec = rvec_.dump();

        var target_ = new StandardCVPipeline.TrackedTarget();
        pipe.calculatePose(imagePoints, target_);
        System.out.println(target_.cameraRelativePose);

        displayImage(mat2BufferedImage(blank));
    }

    public static BufferedImage mat2BufferedImage(Mat m)
    {
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (m.channels() > 1)
        {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        int bufferSize = m.channels()*m.cols()*m.rows();
        byte[] b = new byte[bufferSize];
        m.get(0, 0, b); // get all the pixels
        BufferedImage image = new BufferedImage(m.cols(), m.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(b, 0, targetPixels, 0, b.length);
        return image;
    }

    public static void displayImage(Image img) throws InterruptedException {
        ImageIcon icon = new ImageIcon(img);
        JFrame frame = new JFrame();
        frame.setLayout(new FlowLayout());
        frame.setSize(img.getWidth(null)+50, img.getHeight(null)+50);
        JLabel lbl = new JLabel();
        lbl.setIcon(icon);
        frame.add(lbl);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Thread.sleep(10000);
    }

    private void forceLoad() throws IOException {
        CameraServerJNI.forceLoad();
        CameraServerCvJNI.forceLoad();
    }

}
