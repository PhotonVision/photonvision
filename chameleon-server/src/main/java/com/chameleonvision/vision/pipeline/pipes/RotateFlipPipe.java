package com.chameleonvision.vision.pipeline.pipes;

import com.chameleonvision.vision.enums.ImageFlipMode;
import com.chameleonvision.vision.enums.ImageRotationMode;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Core;
import org.opencv.core.Mat;

public class RotateFlipPipe implements Pipe<Mat, Mat> {

    private ImageRotationMode rotation;
    private ImageFlipMode flip;

    private Mat processBuffer = new Mat();
    private Mat outputMat = new Mat();

    public RotateFlipPipe(ImageRotationMode rotation, ImageFlipMode flip) {
        this.rotation = rotation;
        this.flip = flip;
    }

    public void setConfig(ImageRotationMode rotation, ImageFlipMode flip) {
        this.rotation = rotation;
        this.flip = flip;
    }

    @Override
    public Pair<Mat, Long> run(Mat input) {
        long processStartNanos = System.nanoTime();

        boolean shouldFlip = !flip.equals(ImageFlipMode.NONE);
        boolean shouldRotate = !rotation.equals(ImageRotationMode.DEG_0);

        if (shouldFlip || shouldRotate) {
//            input.copyTo(processBuffer);

            if (shouldFlip) {
                Core.flip(input, input, flip.value);
            }

            if (shouldRotate) {
                Core.rotate(input, input, rotation.value);
            }

//            processBuffer.copyTo(outputMat);
//            processBuffer.release();
        } else {
//            input.copyTo(outputMat);
        }

        long processTime = System.nanoTime() - processStartNanos;
        return Pair.of(input, processTime);
    }
}
