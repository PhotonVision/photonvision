package org.photonvision._2.vision.pipeline.pipes;

import org.photonvision._2.vision.pipeline.Pipe;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;

public class BlurPipe implements Pipe<Mat, Mat> {

    private int blurSize;

    private Mat processBuffer = new Mat();
    private Mat outputMat = new Mat();

    public BlurPipe(int blurSize) {
        this.blurSize = blurSize;
    }

    public void setConfig(int blurSize) {
        this.blurSize = blurSize;
    }

    @Override
    public Pair<Mat, Long> run(Mat input) {
        long processStartNanos = System.nanoTime();

        //        if (blurSize > 0) {
        //            input.copyTo(processBuffer);
        //            try {
        //                Imgproc.blur(processBuffer, processBuffer, new Size(blurSize, blurSize));
        //                processBuffer.copyTo(outputMat);
        //                processBuffer.release();
        //            } catch (CvException e) {
        //                System.err.println("(BlurPipe) Exception thrown by OpenCV: \n" +
        // e.getMessage());
        //            }
        //        } else {
        //            input.copyTo(outputMat);
        //        }

        long processTime = System.nanoTime() - processStartNanos;
        return Pair.of(input, processTime);
    }
}
