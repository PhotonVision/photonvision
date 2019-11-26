package com.chameleonvision.vision.pipeline.pipes;

import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class ErodeDilatePipe implements Pipe<Mat, Mat> {

    private boolean erode;
    private boolean dilate;
    private Mat kernel;

    private Mat processBuffer = new Mat();
    private Mat outputMat = new Mat();

    public ErodeDilatePipe(boolean erode, boolean dilate, int kernelSize) {
        this.erode = erode;
        this.dilate = dilate;
        kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(kernelSize, kernelSize));
    }

    public void setConfig(boolean erode, boolean dilate, int kernelSize) {
        this.erode = erode;
        this.dilate = dilate;
        kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(kernelSize, kernelSize));
    }

    @Override
    public Pair<Mat, Long> run(Mat input) {
        long processStartNanos = System.nanoTime();

        if (erode || dilate) {
            input.copyTo(processBuffer);

            if (erode) {
                Imgproc.erode(processBuffer, processBuffer, kernel);
            }

            if (dilate) {
                Imgproc.erode(processBuffer, processBuffer, kernel);
            }

            processBuffer.copyTo(outputMat);
            processBuffer.release();
        } else {
            input.copyTo(outputMat);
        }

        long processTime = System.nanoTime() - processStartNanos;
        return Pair.of(outputMat, processTime);
    }
}
