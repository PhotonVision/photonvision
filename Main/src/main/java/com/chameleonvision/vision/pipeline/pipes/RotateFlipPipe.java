package com.chameleonvision.vision.pipeline.pipes;

import com.chameleonvision.vision.enums.ImageFlipMode;
import com.chameleonvision.vision.enums.ImageRotation;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Core;
import org.opencv.core.Mat;

public class RotateFlipPipe implements Pipe<Mat, Mat> {

    private ImageRotation rotation;
    private ImageFlipMode flip;

    private Mat processBuffer = new Mat();
    private Mat outputMat = new Mat();

    public RotateFlipPipe(ImageRotation rotation, ImageFlipMode flip) {
        this.rotation = rotation;
        this.flip = flip;
    }

    public void setConfig(ImageRotation rotation, ImageFlipMode flip) {
        this.rotation = rotation;
        this.flip = flip;
    }

    @Override
    public Pair<Mat, Long> run(Mat input) {
        long processStartNanos = System.nanoTime();

        Core.flip(input, processBuffer, flip.value);
        Core.rotate(processBuffer, processBuffer, rotation.value);

        long processTime = System.nanoTime() - processStartNanos;
        Pair<Mat, Long> output = Pair.of(outputMat, processTime);
        processBuffer.release();
        return output;
    }
}
