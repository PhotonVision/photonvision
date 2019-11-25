package com.chameleonvision.vision.pipeline.pipes;

import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;

public class OutputMatPipe implements Pipe<Pair<Mat, Mat>, Mat> {

    private boolean showThresholded;

    private Mat outputMat = new Mat();

    public OutputMatPipe(boolean showThresholded) {
        this.showThresholded = showThresholded;
    }

    public void setConfig(boolean showThresholded) {
        this.showThresholded = showThresholded;
    }

    @Override
    public Pair<Mat, Long> run(Pair<Mat, Mat> input) {
        long processStartNanos = System.nanoTime();

		outputMat = showThresholded ? input.getRight() : input.getLeft();

        long processTime = System.nanoTime() - processStartNanos;
        Pair<Mat, Long> output = Pair.of(outputMat, processTime);
        outputMat.release();
        return output;
    }
}
