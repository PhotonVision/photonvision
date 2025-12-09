/*
 * Copyright (C) Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.photonvision.vision.frame.provider;

import java.nio.file.Path;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.opencv.CVMat;

/**
 * A {@link FrameProvider} that will read and provide an image from a {@link java.nio.file.Path
 * path}.
 */
public class VideoFrameProvider extends CpuImageProcessor {
    private static final Logger logger = new Logger(VideoFrameProvider.class, LogGroup.Camera);

    private final FrameStaticProperties properties;
    private Path path;
    private VideoCapture reader;

    private long lastGetMillis = System.currentTimeMillis();
    private final int millisDelay;

    public VideoFrameProvider(Path path, double fov, CameraCalibrationCoefficients calibration) {
        this.path = path;

        String absPath = path.toAbsolutePath().toString();
        this.reader = new VideoCapture(absPath);

        if (!reader.isOpened()) {
            logger.error("Failed to open video file: " + absPath);
            throw new IllegalArgumentException("Cannot open video file: " + absPath);
        }

        logger.info(
                "Opened video file: "
                        + path.toAbsolutePath()
                        + " using backend "
                        + reader.getBackendName());

        // Get FPS
        var fps = reader.get(Videoio.CAP_PROP_FPS);
        if (fps <= 0) {
            logger.warn("Could not determine FPS, defaulting to 30");
        }
        this.millisDelay = (int) (1000 / fps);

        // Figure out resolution of the video file
        var rawImage = new CVMat();
        reader.read(rawImage.getMat());
        this.properties =
                new FrameStaticProperties(
                        rawImage.getMat().width(), rawImage.getMat().height(), fov, calibration);
        rawImage.release();
    }

    @Override
    public CapturedFrame getInputMat() {
        // sleep to match fps
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

        var out = new CVMat();

        boolean read = reader.read(out.getMat());
        if (!read) {
            // loop
            logger.info("Rewinding video file for next tick: " + path.toAbsolutePath());
            reader.release();
            reader = new VideoCapture(path.toAbsolutePath().toString());
        }

        return new CapturedFrame(out, properties, MathUtils.wpiNanoTime());
    }

    @Override
    public String getName() {
        return "FileFrameProvider-" + this.path;
    }

    @Override
    public void release() {
        reader.release();
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
}
