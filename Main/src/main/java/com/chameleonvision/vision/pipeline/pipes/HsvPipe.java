package com.chameleonvision.vision.pipeline.pipes;

import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class HsvPipe implements Pipe<Mat, Mat> {

    private final Scalar hsvLower, hsvUpper;

    private Mat processBuffer = new Mat();
    private Mat outputMat = new Mat();

    public HsvPipe(Scalar hsvLower, Scalar hsvUpper) {
        this.hsvLower = hsvLower;
        this.hsvUpper = hsvUpper;
    }

    @Override
    public Pair<Mat, Long> run(Mat input) {
        long processStartNanos = System.nanoTime();

        // convert from rgb to hsv
        if(input.empty()) {
            throw new RuntimeException("HSV input cannot be empty!");
        }
        Imgproc.cvtColor(input, processBuffer, Imgproc.COLOR_RGB2HSV, 3);

        Core.inRange(processBuffer, hsvLower, hsvUpper, processBuffer);

        long processTime = processStartNanos - System.nanoTime();
        processBuffer.copyTo(outputMat);
        Pair<Mat, Long> output = Pair.of(outputMat, processTime);
        processBuffer.release();
        return output;
    }
}

