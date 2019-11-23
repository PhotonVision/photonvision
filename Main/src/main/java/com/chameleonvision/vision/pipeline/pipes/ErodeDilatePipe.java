package com.chameleonvision.vision.pipeline.pipes;

import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class ErodeDilatePipe implements Pipe<Mat, Mat> {

    private final boolean erode, dilate;
    private final Mat kernel;

    private Mat outputMat = new Mat();

    public ErodeDilatePipe(boolean erode, boolean dilate, int kernelSize) {
        this.erode = erode;
        this.dilate = dilate;
        kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(kernelSize, kernelSize));
    }

    @Override
    public Pair<Mat, Long> run(Mat input) {
        long processStartNanos = System.nanoTime();

        if (erode) {
            Imgproc.erode(outputMat, outputMat, kernel);
        }

        if (dilate) {
            Imgproc.erode(outputMat, outputMat, kernel);
        }

        long processTime = processStartNanos - System.nanoTime();
        Pair<Mat, Long> output = Pair.of(outputMat, processTime);
        outputMat.release();
        return output;
    }
}
