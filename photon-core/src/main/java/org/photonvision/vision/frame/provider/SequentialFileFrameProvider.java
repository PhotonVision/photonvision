package org.photonvision.vision.frame.provider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.opencv.CVMat;

public class SequentialFileFrameProvider extends CpuImageProcessor {
    public static final int MAX_FPS = 10;
    private static int count = 0;

    private final int thisIndex = count++;
    private final Path directoryPath;
    private final int millisDelay;
    private final List<CVMat> originalFrames;
    private int currentFrame;

    private final FrameStaticProperties properties;

    private long lastGetMillis = System.currentTimeMillis();

    /**
     * Instantiates a new FileFrameProvider.
     *
     * @param directoryPath The path of the directory of images to read from.
     * @param fov The fov of the image.
     * @param maxFPS The max framerate to provide the image at.
     */
    public SequentialFileFrameProvider(Path directoryPath, double fov, int maxFPS) {
        this(directoryPath, fov, maxFPS, null);
    }

    public SequentialFileFrameProvider(
            Path directoryPath, double fov, CameraCalibrationCoefficients calibration) {
        this(directoryPath, fov, MAX_FPS, calibration);
    }

    public SequentialFileFrameProvider(
            Path directoryPath, double fov, int maxFPS, CameraCalibrationCoefficients calibration) {
        if (!Files.exists(directoryPath))
            throw new RuntimeException(
                    "Invalid path for image set, does not exist: " + directoryPath.toAbsolutePath());
        if (!Files.isDirectory(directoryPath))
            throw new RuntimeException(
                    "Invalid path for image set, not a directory: " + directoryPath.toAbsolutePath());
        this.directoryPath = directoryPath;
        this.millisDelay = 1000 / maxFPS;

        originalFrames = new ArrayList<>();

        try {
            for (var path : Files.newDirectoryStream(directoryPath)) {
                if (Files.isRegularFile(path)) {
                    Mat rawImage = Imgcodecs.imread(path.toString());
                    if (rawImage.cols() > 0 && rawImage.rows() > 0) {
                        originalFrames.add(new CVMat(rawImage));
                    } else {
                        rawImage.release();
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (originalFrames.size() == 0) {
            throw new RuntimeException("No images found!");
        }

        properties =
                new FrameStaticProperties(
                        originalFrames.get(0).getMat().width(),
                        originalFrames.get(0).getMat().height(),
                        fov,
                        calibration);

        for (var originalFrame : originalFrames) {
            if (originalFrame.getMat().width() != properties.imageWidth
                    || originalFrame.getMat().height() != properties.imageHeight) {
                originalFrames.forEach(CVMat::release);
                throw new RuntimeException(
                        String.format(
                                "Mismatched calibration image sizes, expected (%d, %d), got (%d, %d)",
                                properties.imageWidth,
                                properties.imageHeight,
                                originalFrame.getMat().width(),
                                originalFrame.getMat().height()));
            }
        }
    }

    /**
     * Instantiates a new File frame provider.
     *
     * @param directoryPathAsString The path of the directory of images to read from as a string.
     * @param fov The fov of the image.
     */
    public SequentialFileFrameProvider(String directoryPathAsString, double fov) {
        this(Paths.get(directoryPathAsString), fov, MAX_FPS);
    }

    /**
     * Instantiates a new File frame provider.
     *
     * @param directoryPath The path of the directory of images to read from.
     * @param fov The fov of the image.
     */
    public SequentialFileFrameProvider(Path directoryPath, double fov) {
        this(directoryPath, fov, MAX_FPS);
    }

    @Override
    public CapturedFrame getInputMat() {
        var out = new CVMat();
        out.copyFrom(originalFrames.get(currentFrame++));
        currentFrame = currentFrame % originalFrames.size();

        // block to keep FPS at a defined rate
        if (System.currentTimeMillis() - lastGetMillis < millisDelay) {
            try {
                Thread.sleep(millisDelay);
            } catch (InterruptedException e) {
                System.err.println("FileFrameProvider interrupted - not busywaiting");
                // throw back up the stack
                throw new RuntimeException(e);
            }
        }

        lastGetMillis = System.currentTimeMillis();
        return new CapturedFrame(out, properties, MathUtils.wpiNanoTime());
    }

    @Override
    public String getName() {
        return "FileFrameProvider" + thisIndex + " - " + directoryPath.getFileName();
    }

    @Override
    public void release() {
        originalFrames.forEach(CVMat::release);
    }

    @Override
    public boolean checkCameraConnected() {
        return true;
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public boolean hasConnected() {
        return true;
    }

    public boolean atStart() {
        return currentFrame == 0;
    }

    public Size resolution() {
        return new Size(properties.imageWidth, properties.imageHeight);
    }
}
