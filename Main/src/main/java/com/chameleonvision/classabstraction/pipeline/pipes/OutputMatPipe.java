package com.chameleonvision.classabstraction.pipeline.pipes;

import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;

public class OutputMatPipe implements Pipe<Pair<Mat, Mat>, Mat> {

    private boolean showThresholded;

    private Mat outputMat = new Mat();

    public OutputMatPipe(boolean showThresholded) {
        this.showThresholded = showThresholded;
    }

    @Override
    public Pair<Mat, Long> run(Pair<Mat, Mat> input) {
        long processStartNanos = System.nanoTime();

        if (showThresholded) {
            input.getRight().copyTo(outputMat);
        } else {
            input.getLeft().copyTo(outputMat);
        }

        long processTime = processStartNanos - System.nanoTime();
        Pair<Mat, Long> output = Pair.of(outputMat, processTime);
        outputMat.release();
        return output;
    }
}
