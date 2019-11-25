package com.chameleonvision.vision.pipeline.pipes;

import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class BlurPipe implements Pipe<Mat, Mat> {

    private final int blurSize;

    private Mat processBuffer = new Mat();
    private Mat outputMat = new Mat();

    public BlurPipe(int blurSize) {
        this.blurSize = blurSize;
    }

    @Override
    public Pair<Mat, Long> run(Mat input) {
        long processStartNanos = System.nanoTime();

        try {
            if (blurSize > 0) {
                Imgproc.blur(outputMat, outputMat, new Size(blurSize, blurSize));
            }
        } catch (CvException e) {
            System.err.println("(BlurPipe) Exception thrown by OpenCV: \n" + e.getMessage());
        }

        long processTime = System.nanoTime() - processStartNanos;
        Pair<Mat, Long> output = Pair.of(outputMat, processTime);
        processBuffer.release();

        return output;
    }
}
