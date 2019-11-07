package com.chameleonvision.classabstraction.pipeline.pipes;

import com.chameleonvision.vision.ImageFlipMode;
import com.chameleonvision.vision.ImageRotation;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Core;
import org.opencv.core.Mat;

public class RotateFlipPipe implements Pipe<Mat, Mat> {

    private final ImageRotation rotation;
    private final ImageFlipMode flip;

    private Mat outputMat = new Mat();

    public RotateFlipPipe(ImageRotation rotation, ImageFlipMode flip) {
        this.rotation = rotation;
        this.flip = flip;
    }

    @Override
    public Pair<Mat, Long> run(Mat input) {
        long processStartNanos = System.nanoTime();

        Core.flip(input, outputMat, flip.value);
        Core.rotate(outputMat, outputMat, rotation.value);

        long processTime = processStartNanos - System.nanoTime();
        Pair<Mat, Long> output = Pair.of(outputMat, processTime);
        outputMat.release();
        return output;
    }
}
