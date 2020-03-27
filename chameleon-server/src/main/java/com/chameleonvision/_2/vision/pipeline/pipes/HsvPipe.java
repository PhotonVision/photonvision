package com.chameleonvision._2.vision.pipeline.pipes;

import com.chameleonvision._2.vision.pipeline.Pipe;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class HsvPipe implements Pipe<Mat, Mat> {

    private Scalar hsvLower;
    private Scalar hsvUpper;

    private Mat processBuffer = new Mat();
    private Mat outputMat = new Mat();

    public HsvPipe(Scalar hsvLower, Scalar hsvUpper) {
        this.hsvLower = hsvLower;
        this.hsvUpper = hsvUpper;
    }

    public void setConfig(Scalar hsvLower, Scalar hsvUpper) {
        this.hsvLower = hsvLower;
        this.hsvUpper = hsvUpper;
    }

    @Override
    public Pair<Mat, Long> run(Mat input) {
        long processStartNanos = System.nanoTime();

        input.copyTo(outputMat);

        try {
            Imgproc.cvtColor(outputMat, outputMat, Imgproc.COLOR_BGR2HSV, 3);
            Core.inRange(outputMat, hsvLower, hsvUpper, outputMat);
        } catch (CvException e) {
            System.err.println("(HsvPipe) Exception thrown by OpenCV: \n" + e.getMessage());
        }

        long processTime = System.nanoTime() - processStartNanos;
        return Pair.of(outputMat, processTime);
    }
}

