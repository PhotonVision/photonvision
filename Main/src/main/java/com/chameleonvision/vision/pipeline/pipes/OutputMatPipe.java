package com.chameleonvision.vision.pipeline.pipes;

import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class OutputMatPipe implements Pipe<Pair<Mat, Mat>, Mat> {

    private boolean showThresholded;

    private Mat processBuffer = new Mat();
    private Mat outputMat = new Mat();

    public OutputMatPipe(boolean showThresholded) {
        this.showThresholded = showThresholded;
    }

    public void setConfig(boolean showThresholded) {
        this.showThresholded = showThresholded;
    }

    /**
     *
     * @param input Input object for pipe
     *        Left is raw camera mat (8UC3), Right is HSV threshold mat (8UC1)
     * @return Returns desired output Mat, and processing time in nanoseconds
     */
    @Override
    public Pair<Mat, Long> run(Pair<Mat, Mat> input) {
        long processStartNanos = System.nanoTime();

        if (showThresholded) {
            try {
                input.getRight().copyTo(processBuffer);
                Imgproc.cvtColor(processBuffer, processBuffer, Imgproc.COLOR_GRAY2BGR, 3);
                processBuffer.copyTo(outputMat);
                processBuffer.release();
            } catch (CvException e) {
                System.err.println("(OutputMat) Exception thrown by OpenCV: \n" + e.getMessage());
            }
        } else {
            input.getLeft().copyTo(outputMat);
        }

        long processTime = System.nanoTime() - processStartNanos;
        return Pair.of(outputMat, processTime);
    }
}
