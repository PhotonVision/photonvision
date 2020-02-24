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
                new JsonMat(3, 3, 6, new double[] { 926.1016601017006, 0.0, 437.22446072361055, 0.0, 918.2612433944396, 137.8989492231747, 0.0, 0.0, 1.0 }),
                new JsonMat(1, 5, 6, new double[] { -0.001917838248173303, 0.0059895823594355, -0.035282888419499406, 0.04373249383460662, 0.1732528905031391 }),
                1.056
                );

        var pipe = new SolvePNPPipe(settings, calibration, new Rotation2d());

        // project pts
        var target = settings.targetCornerMat;
        Mat blank = Imgcodecs.imread(Path.of(root.toString(), "black.png").toString());

        // make a tvec and rvec
//        positive x to the right, positive y to the bottom, positive z away from the image
        // tvec example:
//        [45.92180012026041;
//        24.94979431168253;
//        88.00905002660249]
        // rvec:
//        [1.990867004634147;
//        -0.1508389335122144;
//        -1.552061845576413]

        Mat tvec = new Mat(3, 1, 6);
        tvec.put(0, 0, 0, 0, 200); // 10ft away?
        Mat rvec = new Mat(3, 1, 6);
        rvec.put(0, 0, 0, 0, 0);

        MatOfPoint2f imagePoints = new MatOfPoint2f();
        Calib3d.projectPoints(target, rvec, tvec, calibration.getCameraMatrixAsMat(), calibration.getDistortionCoeffsAsMat(), imagePoints, new Mat(), 0);
        var projectedPts = imagePoints.toList();

        for(var p: projectedPts) {
            Imgproc.circle(blank, p, 3, new Scalar(0, 0, 255), 4);
        }

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
