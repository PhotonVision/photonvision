package com.chameleonvision.classabstraction.pipeline.pipes;

import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class BlurPipe implements Pipe<Mat, Mat> {

    private final int blurSize;

    private Mat outputMat = new Mat();

    public BlurPipe(int blurSize) {
        this.blurSize = blurSize;
    }

    @Override
    public Pair<Mat, Long> run(Mat input) {
        long processStartNanos = System.nanoTime();

        if (blurSize > 0) {
            Imgproc.blur(outputMat, outputMat, new Size(blurSize, blurSize));
        }

        long processTime = processStartNanos - System.nanoTime();
        Pair<Mat, Long> output = Pair.of(outputMat, processTime);
        outputMat.release();
        return output;
    }
}
