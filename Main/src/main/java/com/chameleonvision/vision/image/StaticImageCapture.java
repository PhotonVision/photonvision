package com.chameleonvision.vision.image;

import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.nio.file.Files;
import java.nio.file.Path;

public class StaticImageCapture implements ImageCapture {

    private final Mat image = new Mat();

    public StaticImageCapture(Path imagePath) {
        if (!Files.exists(imagePath)) throw new RuntimeException("Invalid path for image!");

        Mat tempMat = new Mat();

        try {
            tempMat = Imgcodecs.imread(imagePath.toString());
        } catch (Exception e) {
            System.err.println("Failed to read image!");
        } finally {
            tempMat.copyTo(image);
        }
    }

    @Override
    public Pair<Mat, Long> getFrame() {
        return Pair.of(image, System.nanoTime());
    }
}
