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

package org.photonvision.vision.pipe.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CharucoBoard;
import org.opencv.objdetect.CharucoDetector;
import org.opencv.objdetect.Objdetect;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.frame.FrameDivisor;
import org.photonvision.vision.opencv.Releasable;
import org.photonvision.vision.pipe.CVPipe;
import org.photonvision.vision.pipeline.UICalibrationData;
import org.wpilib.math.util.Pair;

public class FindBoardCornersPipe
        extends CVPipe<
                Pair<Mat, Mat>,
                FindBoardCornersPipe.FindBoardCornersPipeResult,
                FindBoardCornersPipe.FindCornersPipeParams> {
    private static final Logger logger =
            new Logger(FindBoardCornersPipe.class, LogGroup.VisionModule);

    Optional<MatOfPoint3f> objectPointsTemplate = Optional.empty();

    Size imageSize;
    Size patternSize;

    CharucoBoard board;
    CharucoDetector detector;

    // Configure the optimizations used while using OpenCV's find corners algorithm
    // Since we return results in real-time, we want to ensure it goes as fast as
    // possible
    // and fails as fast as possible.
    static final int findChessboardFlags =
            Calib3d.CALIB_CB_NORMALIZE_IMAGE
                    | Calib3d.CALIB_CB_ADAPTIVE_THRESH
                    | Calib3d.CALIB_CB_FILTER_QUADS
                    | Calib3d.CALIB_CB_FAST_CHECK;

    protected final Mat detectedCorners = new Mat();

    // Intermediate result mat's
    protected final Mat smallerInFrame = new Mat();
    protected final MatOfPoint2f smallerImagePoints = new MatOfPoint2f();

    // SubCornerPix params
    private final Size zeroZone = new Size(-1, -1);
    private final TermCriteria criteria = new TermCriteria(3, 30, 0.001);

    @Override
    public void setParams(FindCornersPipeParams params) {
        super.setParams(params);

        objectPointsTemplate.ifPresent(
                mat -> {
                    mat.release();
                    objectPointsTemplate = Optional.empty();
                });

        /*
         * If using a chessboard, then the pattern size is the inner corners of the
         * board. For example, the pattern size of a 9x9 chessboard would be 8x8
         * We subtract 1 for chessboard because the UI prompts users for the number of
         * squares, not the number of corners.
         */
        patternSize =
                params.type() == UICalibrationData.BoardType.CHESSBOARD
                        ? new Size(params.boardWidth() - 1, params.boardHeight() - 1)
                        : new Size(params.boardWidth(), params.boardHeight());

        if (params.type() == UICalibrationData.BoardType.CHARUCOBOARD) {
            board =
                    new CharucoBoard(
                            new Size(params.boardWidth(), params.boardHeight()),
                            (float) params.gridSize(),
                            (float) params.markerSize(),
                            Objdetect.getPredefinedDictionary(params.tagFamily().getValue()));
            board.setLegacyPattern(params.useOldPattern());
            detector = new CharucoDetector(board);
            detector.getDetectorParameters().set_adaptiveThreshConstant(10);
            detector.getDetectorParameters().set_adaptiveThreshWinSizeMin(11);
            detector.getDetectorParameters().set_adaptiveThreshWinSizeStep(40);
            detector.getDetectorParameters().set_adaptiveThreshWinSizeMax(91);
        } else {
            board = null;
            detector = null;
        }
    }

    protected MatOfPoint3f createObjectPoints() {
        switch (params.type()) {
            case CHESSBOARD:
                if (objectPointsTemplate.isEmpty()) {
                    var objectPoints = new Point3[(int) patternSize.height * (int) patternSize.width];
                    int i = 0;
                    for (int heightIdx = 0; heightIdx < patternSize.height; heightIdx++) {
                        for (int widthIdx = 0; widthIdx < patternSize.width; widthIdx++) {
                            double boardYCoord = heightIdx * params.gridSize();
                            double boardXCoord = widthIdx * params.gridSize();
                            objectPoints[i++] = new Point3(boardXCoord, boardYCoord, 0.0);
                        }
                    }
                    objectPointsTemplate = Optional.of(new MatOfPoint3f(objectPoints));
                }
            case CHARUCOBOARD:
                // Since ChArUco boards support partial observations, object points can't be cached
                break;
            default:
                logger.error("Can't create pattern for unknown board type " + params.type());
                break;
        }

        // If the template exists, create a shallow copy. Otherwise, create a new distinct matrix.
        return objectPointsTemplate
                .map(mat -> new MatOfPoint3f(mat))
                .orElseGet(() -> new MatOfPoint3f());
    }

    /**
     * Finds the corners in a given image and returns them
     *
     * @param in Input for pipe processing. Pair of input and output mat
     * @return All valid Mats for camera calibration
     */
    @Override
    protected FindBoardCornersPipeResult process(Pair<Mat, Mat> in) {
        return findBoardCorners(in);
    }

    /**
     * Figures out how much a frame or point cloud must be scaled down by to match the desired size at
     * which to run FindCorners. Should usually be > 1.
     *
     * @param inFrame
     * @return
     */
    private double getFindCornersScaleFactor(Mat inFrame) {
        return 1.0 / params.divisor().value;
    }

    /**
     * Finds the minimum spacing between a set of x/y points Currently only considers points whose
     * index is next to each other Which, currently, means it traverses one dimension. This is a rough
     * heuristic approach which could be refined in the future.
     *
     * <p>Note that the current implementation can be fooled under the following conditions: (1) The
     * width of the image is an odd number, and the smallest distance was actually on the between the
     * last two points in a given row and (2) The smallest distance was actually in the direction
     * orthogonal to that which was getting traversed by iterating through the MatOfPoint2f in order.
     *
     * <p>I've chosen not to handle these for speed's sake, and because, really, you don't need the
     * exact answer for "min distance". you just need something fairly reasonable.
     *
     * @param inPoints point set to analyze. Must be a "tall" matrix.
     * @return min spacing between neighbors
     */
    private double getApproxMinSpacing(MatOfPoint2f inPoints) {
        double minSpacing = Double.MAX_VALUE;
        for (int pointIdx = 0; pointIdx < inPoints.height() - 1; pointIdx += 2) {
            // +1 idx Neighbor distance
            double[] startPoint = inPoints.get(pointIdx, 0);
            double[] endPoint = inPoints.get(pointIdx + 1, 0);
            double distToNext = Math.hypot(startPoint[0] - endPoint[0], startPoint[1] - endPoint[1]);

            minSpacing = Math.min(distToNext, minSpacing);
        }
        return minSpacing;
    }

    /**
     * @param inFrame Full-size mat that is going to get scaled down before passing to
     *     findBoardCorners
     * @return the size to scale the input mat to
     */
    private Size getFindCornersImgSize(Mat in) {
        int width = in.cols() / params.divisor().value;
        int height = in.rows() / params.divisor().value;
        return new Size(width, height);
    }

    /**
     * Gets the decimation level, equal to the divisor on a log-base-2 scale
     *
     * @return the decimation level
     */
    private float getDecimationLevel() {
        return switch (params.divisor()) {
            case NONE -> 0.0f;
            case HALF -> 1.0f;
            case QUARTER -> 2.0f;
            default -> (float) (Math.log(params.divisor().value) / Math.log(2.0));
        };
    }

    /**
     * Given an input frame and a set of points from the "smaller" findChessboardCorner analysis,
     * re-scale the points back to where they would have been in the input frame
     *
     * @param inPoints set of points derived from a call to findChessboardCorner on a shrunken mat.
     *     Must be a "tall" matrix.
     * @param origFrame Original frame we're rescaling points back to
     * @param outPoints mat into which the output rescaled points get placed
     */
    private void rescalePointsToOrigFrame(
            MatOfPoint2f inPoints, Mat origFrame, MatOfPoint2f outPoints) {
        // Rescale boardCorners back up to the inproc image size
        Point[] outPointsArr = new Point[inPoints.height()];
        double sf = getFindCornersScaleFactor(origFrame);
        for (int pointIdx = 0; pointIdx < inPoints.height(); pointIdx++) {
            double[] pointCoords = inPoints.get(pointIdx, 0);
            double outXCoord = pointCoords[0] / sf;
            double outYCoord = pointCoords[1] / sf;
            outPointsArr[pointIdx] = new Point(outXCoord, outYCoord);
        }
        outPoints.fromArray(outPointsArr);
    }

    /**
     * Picks a window size for doing subpixel optimization based on the board type and spacing
     * observed between the corners or points in the image
     *
     * @param inPoints
     * @return
     */
    private Size getWindowSize(MatOfPoint2f inPoints) {
        double windowHalfWidth = 11; // Dot board uses fixed-size window half-width
        if (params.type() == UICalibrationData.BoardType.CHESSBOARD) {
            // Chessboard uses a dynamic sized window based on how far apart the corners are
            windowHalfWidth = Math.floor(getApproxMinSpacing(inPoints) * 0.50);
            windowHalfWidth = Math.max(1, windowHalfWidth);
        }
        return new Size(windowHalfWidth, windowHalfWidth);
    }

    /**
     * Find chessboard corners given an input mat and output mat to draw on
     *
     * @return Frame resolution, object points, board corners
     */
    private FindBoardCornersPipeResult findBoardCorners(Pair<Mat, Mat> in) {
        /** 3 dimensional, the physical board point coordinates, Z is always zero for a flat board */
        final MatOfPoint3f objectPoints = createObjectPoints();
        /**
         * 2 dimensional, the likely distorted board on the flat camera sensor frame posed relative to
         * the target board on the flat camera sensor frame posed relative to the target
         */
        final MatOfPoint2f imagePoints = new MatOfPoint2f();
        /** Decimation level that the points were captured at */
        float level = getDecimationLevel();
        /** Ids of each of the corners */
        MatOfInt ids = null;

        final Mat inFrame = in.getFirst();
        final Mat outFrame = in.getSecond();

        // Convert the inFrame to grayscale to increase contrast
        Imgproc.cvtColor(inFrame, inFrame, Imgproc.COLOR_BGR2GRAY);
        boolean boardFound = false;

        // Get the size of the inFrame
        this.imageSize = new Size(inFrame.width(), inFrame.height());

        switch (params.type()) {
            case CHARUCOBOARD:
                ids = new MatOfInt();

                // These Mats are references to existing memory, so there shouldn't be a substantial cost to
                // allocating them
                detector.detectBoard(inFrame, detectedCorners, ids);

                // reformat the Mat to a List<Mat> for matchImagePoints
                final List<Mat> detectedCornersList = new ArrayList<>();
                for (int i = 0; i < detectedCorners.total(); i++) {
                    // Each row is a new tracked reference to the same native memory
                    detectedCornersList.add(detectedCorners.row(i));
                }

                if (detectedCornersList.size()
                        >= 10) { // We need at least 4 corners to be used for calibration but we
                    // force 10 just to
                    // ensure the user cant get away with a garbage calibration.
                    boardFound = true;
                }

                if (!boardFound) {
                    // If we can't find a board, give up
                    objectPoints.release();
                    imagePoints.release();
                    ids.release();
                    return null;
                }
                board.matchImagePoints(detectedCornersList, ids, objectPoints, imagePoints);

                detectedCornersList.forEach(row -> row.release());

                // Draw the ChArUco board
                Objdetect.drawDetectedCornersCharuco(
                        outFrame, detectedCorners, ids, new Scalar(0, 0, 255)); // Red Text

                // Mrcal wants our top-left corner at 0, 0. But charuco hands us the first corner at the
                // first board intersection, which is inset a couple mm. Adjust such that the top-left
                // corner is at 0,0
                {
                    // don't trust any particular ordering
                    List<Point3> pointList = objectPoints.toList();
                    double minX = pointList.stream().mapToDouble(p -> p.x).min().orElse(0.0);
                    double minY = pointList.stream().mapToDouble(p -> p.y).min().orElse(0.0);

                    // Shift all object points so that the origin is at (0,0)
                    List<Point3> shiftedPoints =
                            pointList.stream().map(p -> new Point3(p.x - minX, p.y - minY, p.z)).toList();

                    objectPoints.fromList(shiftedPoints);
                }

                // Decimation was not used
                level = 0.0f;

                break;
            case CHESSBOARD:
                // Reduce the image size to be much more manageable
                // Note that opencv will copy the frame if no resize is requested; we can skip this since we
                // don't need that copy. See:
                // https://github.com/opencv/opencv/blob/a8ec6586118c3f8e8f48549a85f2da7a5b78bcc9/modules/imgproc/src/resize.cpp#L4185
                Mat frame;
                if (params.divisor() != FrameDivisor.NONE) {
                    Imgproc.resize(inFrame, smallerInFrame, getFindCornersImgSize(inFrame));
                    frame = smallerInFrame;
                } else {
                    frame = inFrame;
                }

                // Run the chessboard corner finder on the smaller image
                boardFound =
                        Calib3d.findChessboardCorners(
                                frame, patternSize, smallerImagePoints, findChessboardFlags);

                if (!boardFound) {
                    objectPoints.release();
                    imagePoints.release();
                    return null;
                }

                rescalePointsToOrigFrame(smallerImagePoints, inFrame, imagePoints);

                // Do sub corner pix for drawing chessboard when using OpenCV
                Imgproc.cornerSubPix(inFrame, imagePoints, getWindowSize(imagePoints), zeroZone, criteria);

                // draw the chessboard, doesn't have to be different for a dot board since it just
                // reprojects the corners we found
                Calib3d.drawChessboardCorners(outFrame, patternSize, imagePoints, true);

                break;
        }

        if (!boardFound) {
            objectPoints.release();
            imagePoints.release();
            if (ids != null) ids.release();
            // If we can't find a calibration board, give up
            return null;
        }

        return new FindBoardCornersPipeResult(inFrame.size(), objectPoints, imagePoints, level, ids);
    }

    @Override
    public void release() {
        objectPointsTemplate.ifPresent(mat -> mat.release());
        detectedCorners.release();
        smallerInFrame.release();
        smallerImagePoints.release();
    }

    public static record FindCornersPipeParams(
            int boardHeight,
            int boardWidth,
            UICalibrationData.BoardType type,
            UICalibrationData.TagFamily tagFamily,
            double gridSize, // meters
            double markerSize, // meters
            FrameDivisor divisor,
            boolean useOldPattern) {}

    public static class FindBoardCornersPipeResult implements Releasable {
        public final Size size;
        public final MatOfPoint3f objectPoints;
        public final MatOfPoint2f imagePoints;
        public final float level;
        public final MatOfInt ids;

        // Set later only if we need it
        public Mat inputImage = null;

        public FindBoardCornersPipeResult(
                Size size, MatOfPoint3f objectPoints, MatOfPoint2f imagePoints, float level, MatOfInt ids) {
            this.size = size;
            this.objectPoints = objectPoints;
            this.imagePoints = imagePoints;
            this.level = level;
            this.ids = ids;
        }

        public FindBoardCornersPipeResult clone() {
            var result =
                    new FindBoardCornersPipeResult(
                            size,
                            new MatOfPoint3f(objectPoints),
                            new MatOfPoint2f(imagePoints),
                            level,
                            ids != null ? new MatOfInt(ids) : null);
            result.inputImage = this.inputImage;
            return result;
        }

        @Override
        public void release() {
            objectPoints.release();
            imagePoints.release();
            if (ids != null) ids.release();
            if (inputImage != null) inputImage.release();
        }
    }
}
