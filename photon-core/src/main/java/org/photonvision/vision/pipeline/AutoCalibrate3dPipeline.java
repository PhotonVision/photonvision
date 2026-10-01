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

package org.photonvision.vision.pipeline;

import java.util.ArrayDeque;
import java.util.List;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.ImageRotationMode;
import org.photonvision.vision.pipe.impl.FindBoardCornersPipe.FindBoardCornersPipeResult;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.pipeline.result.CalibrationPipelineResult;
import org.wpilib.util.Pair;

/**
 * A variant of {@link Calibrate3dPipeline} that takes snapshots automatically. A snapshot is taken
 * once the board's centroid has moved at least a minimum distance from the last snapshot (or from
 * the start of calibration), and only once the board has been stable for the last few frames to
 * avoid motion blur.
 */
public class AutoCalibrate3dPipeline extends Calibrate3dPipeline {
    private static final Logger logger = new Logger(AutoCalibrate3dPipeline.class, LogGroup.General);

    private static final double MIN_MOVEMENT_FRACTION = 0.05;

    private static final double STABILITY_FRACTION = 0.01;

    private static final int STABILITY_WINDOW = 3;

    private final ArrayDeque<Point> recentCentroids = new ArrayDeque<>();

    private Point lastSnapshotCentroid = null;

    public AutoCalibrate3dPipeline() {
        super();
    }

    @Override
    protected boolean isAutoCalibration() {
        return true;
    }

    @Override
    public void finishCalibration() {
        super.finishCalibration();
        recentCentroids.clear();
        lastSnapshotCentroid = null;
    }

    @Override
    protected CVPipelineResult process(Frame frame, Calibration3dPipelineSettings settings) {
        Mat inputColorMat = frame.colorImage.getMat();

        if (this.calibrating || inputColorMat.empty()) {
            return new CVPipelineResult(frame.sequenceID, 0, 0, null, frame);
        }

        if (getSettings().inputImageRotationMode != ImageRotationMode.DEG_0) {
            // All this calibration assumes zero rotation. If we want a rotation, it should
            // be applied at
            // the output
            logger.error(
                    "Input image rotation was non-zero! Calibration wasn't designed to deal with this. Attempting to manually change back to zero");
            getSettings().inputImageRotationMode = ImageRotationMode.DEG_0;
            return new CVPipelineResult(frame.sequenceID, 0, 0, List.of(), frame);
        }

        long sumPipeNanosElapsed = 0L;

        // Check if the frame has chessboard corners
        var outputColorCVMat = new CVMat();
        inputColorMat.copyTo(outputColorCVMat.getMat());

        FindBoardCornersPipeResult findBoardResult =
                findBoardCornersPipe.run(Pair.of(inputColorMat, outputColorCVMat.getMat())).output;

        if (findBoardResult != null) {
            Point centroid = computeCentroid(findBoardResult.imagePoints.toList());
            double frameDiagonal = Math.hypot(findBoardResult.size.width, findBoardResult.size.height);
            maybeTakeSnapshot(findBoardResult, inputColorMat, centroid, frameDiagonal);
        } else {
            // No board visible -- don't let stale positions count towards stability
            recentCentroids.clear();
        }

        var fpsResult = calculateFPSPipe.run(null);
        var fps = fpsResult.output;

        frame.release();

        // Return the drawn chessboard if corners are found, if not, then return the
        // input image.
        return new CalibrationPipelineResult(
                frame.sequenceID,
                sumPipeNanosElapsed,
                fps, // Unused but here in case
                new Frame(
                        frame.sequenceID,
                        new CVMat(),
                        outputColorCVMat,
                        FrameThresholdType.NONE,
                        frame.frameStaticProperties),
                getCornersList());
    }

    /**
     * Takes a snapshot if the board has moved far enough from the last snapshot and has been stable
     * over the last {@link #STABILITY_WINDOW} detections.
     */
    private void maybeTakeSnapshot(
            FindBoardCornersPipeResult findBoardResult,
            Mat inputColorMat,
            Point centroid,
            double frameDiagonal) {
        recentCentroids.addLast(centroid);
        while (recentCentroids.size() > STABILITY_WINDOW) {
            recentCentroids.removeFirst();
        }

        double minMovementPx = MIN_MOVEMENT_FRACTION * frameDiagonal;
        boolean movedEnough =
                lastSnapshotCentroid == null || distance(centroid, lastSnapshotCentroid) >= minMovementPx;
        if (!movedEnough || !isStable(STABILITY_FRACTION * frameDiagonal)) {
            return;
        }

        findBoardResult.inputImage = inputColorMat.clone();

        foundCornersList.add(findBoardResult);
        lastSnapshotCentroid = centroid;
        recentCentroids.clear();

        logger.info("Automatically took calibration snapshot " + foundCornersList.size());
        broadcastState();
    }

    /**
     * True if the last {@link #STABILITY_WINDOW} centroids all sit within {@code maxDeviationPx} of
     * their average.
     */
    private boolean isStable(double maxDeviationPx) {
        if (recentCentroids.size() < STABILITY_WINDOW) {
            return false;
        }

        Point mean = new Point(0, 0);
        for (Point p : recentCentroids) {
            mean.x += p.x;
            mean.y += p.y;
        }
        mean.x /= recentCentroids.size();
        mean.y /= recentCentroids.size();

        for (Point p : recentCentroids) {
            if (distance(p, mean) > maxDeviationPx) {
                return false;
            }
        }
        return true;
    }

    private static Point computeCentroid(List<Point> points) {
        Point centroid = new Point(0, 0);
        for (Point p : points) {
            centroid.x += p.x;
            centroid.y += p.y;
        }
        centroid.x /= points.size();
        centroid.y /= points.size();
        return centroid;
    }

    private static double distance(Point a, Point b) {
        return Math.hypot(a.x - b.x, a.y - b.y);
    }
}
