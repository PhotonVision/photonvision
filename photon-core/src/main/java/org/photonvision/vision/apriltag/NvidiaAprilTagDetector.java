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

package org.photonvision.vision.apriltag;

import edu.wpi.first.apriltag.AprilTagDetection;
import java.util.ArrayList;
import java.util.List;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.jni.NvidiaAprilTagDetection;
import org.photonvision.jni.NvidiaAprilTagJNI;
import org.photonvision.vision.opencv.Releasable;

public class NvidiaAprilTagDetector implements Releasable {
    private static final Logger logger = new Logger(NvidiaAprilTagDetector.class, LogGroup.VisionModule);
    private static final int TILE_SIZE = 4;
    private static final double[] IDENTITY_HOMOGRAPHY = new double[] {1, 0, 0, 0, 1, 0, 0, 0, 1};

    private final Mat colorConversionBuffer = new Mat();
    private final Mat resizeBuffer = new Mat();
    private final MatOfPoint2f homographySource =
            new MatOfPoint2f(
                    new Point(-1, 1),
                    new Point(1, 1),
                    new Point(1, -1),
                    new Point(-1, -1));
    private final MatOfPoint2f homographyDestination = new MatOfPoint2f();

    private long detectorHandle;
    private int detectorWidth = -1;
    private int detectorHeight = -1;

    public List<AprilTagDetection> detect(Mat colorImage, double decimate) {
        if (colorImage.empty()) {
            return List.of();
        }

        var preparedInput = prepareDetectorInput(colorImage, decimate);
        ensureDetector(preparedInput.input().cols(), preparedInput.input().rows());

        var rawDetections = NvidiaAprilTagJNI.detect(detectorHandle, preparedInput.input().nativeObj);
        if (rawDetections == null || rawDetections.length == 0) {
            return List.of();
        }

        var detections = new ArrayList<AprilTagDetection>(rawDetections.length);
        for (var rawDetection : rawDetections) {
            detections.add(
                    toAprilTagDetection(
                            rawDetection, preparedInput.scaleX(), preparedInput.scaleY()));
        }

        return detections;
    }

    public static boolean isRuntimeSupported() {
        return NvidiaAprilTagJNI.isRuntimeSupported();
    }

    @Override
    public void release() {
        destroyDetector();
        colorConversionBuffer.release();
        resizeBuffer.release();
        homographySource.release();
        homographyDestination.release();
    }

    private PreparedInput prepareDetectorInput(Mat colorImage, double decimate) {
        Mat detectorInput;
        if (colorImage.channels() == 3) {
            detectorInput = colorImage;
        } else if (colorImage.channels() == 4) {
            Imgproc.cvtColor(colorImage, colorConversionBuffer, Imgproc.COLOR_BGRA2BGR);
            detectorInput = colorConversionBuffer;
        } else if (colorImage.channels() == 1) {
            Imgproc.cvtColor(colorImage, colorConversionBuffer, Imgproc.COLOR_GRAY2BGR);
            detectorInput = colorConversionBuffer;
        } else {
            throw new IllegalArgumentException(
                    "Unsupported AprilTag input image format with "
                            + colorImage.channels()
                            + " channels");
        }

        if (decimate <= 1.0) {
            return new PreparedInput(detectorInput, 1.0, 1.0);
        }

        var resizedWidth = Math.max(1, (int) Math.round(detectorInput.cols() / decimate));
        var resizedHeight = Math.max(1, (int) Math.round(detectorInput.rows() / decimate));
        if (resizedWidth == detectorInput.cols() && resizedHeight == detectorInput.rows()) {
            return new PreparedInput(detectorInput, 1.0, 1.0);
        }

        Imgproc.resize(
                detectorInput,
                resizeBuffer,
                new Size(resizedWidth, resizedHeight),
                0,
                0,
                Imgproc.INTER_AREA);

        return new PreparedInput(
                resizeBuffer,
                (double) colorImage.cols() / resizedWidth,
                (double) colorImage.rows() / resizedHeight);
    }

    private void ensureDetector(int width, int height) {
        if (detectorHandle != 0 && detectorWidth == width && detectorHeight == height) {
            return;
        }

        destroyDetector();

        detectorHandle = NvidiaAprilTagJNI.createDetector(width, height, TILE_SIZE);
        if (detectorHandle == 0) {
            throw new IllegalStateException("Failed to create NVIDIA AprilTag detector");
        }

        detectorWidth = width;
        detectorHeight = height;
    }

    private void destroyDetector() {
        if (detectorHandle != 0) {
            NvidiaAprilTagJNI.destroyDetector(detectorHandle);
            detectorHandle = 0;
        }

        detectorWidth = -1;
        detectorHeight = -1;
    }

    private AprilTagDetection toAprilTagDetection(
            NvidiaAprilTagDetection detection, double scaleX, double scaleY) {
        var scaledCorners = new double[8];
        for (int i = 0; i < 4; i++) {
            scaledCorners[i * 2] = detection.corners[i * 2] * scaleX;
            scaledCorners[i * 2 + 1] = detection.corners[i * 2 + 1] * scaleY;
        }

        var scaledCenterX = detection.centerX * scaleX;
        var scaledCenterY = detection.centerY * scaleY;

        return new AprilTagDetection(
                "tag36h11",
                detection.id,
                detection.hammingError,
                0.0f,
                computeHomography(scaledCorners),
                scaledCenterX,
                scaledCenterY,
                scaledCorners);
    }

    private double[] computeHomography(double[] corners) {
        homographyDestination.fromArray(
                new Point(corners[0], corners[1]),
                new Point(corners[2], corners[3]),
                new Point(corners[4], corners[5]),
                new Point(corners[6], corners[7]));

        var homography = Imgproc.getPerspectiveTransform(homographySource, homographyDestination);
        if (homography.empty()) {
            logger.warn("Falling back to identity AprilTag homography because OpenCV returned an empty matrix");
            return IDENTITY_HOMOGRAPHY.clone();
        }

        var homographyData = new double[9];
        homography.get(0, 0, homographyData);
        homography.release();
        return homographyData;
    }

    private record PreparedInput(Mat input, double scaleX, double scaleY) {}
}
