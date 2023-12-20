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

import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.mrgingham.MrginghamJNI;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.frame.FrameDivisor;
import org.photonvision.vision.pipe.CVPipe;
import org.photonvision.vision.pipeline.UICalibrationData;

public class FindBoardCornersPipe
        extends CVPipe<
                Pair<Mat, Mat>, Triple<Size, Mat, Mat>, FindBoardCornersPipe.FindCornersPipeParams> {
    private static final Logger logger =
            new Logger(FindBoardCornersPipe.class, LogGroup.VisionModule);

    MatOfPoint3f objectPoints = new MatOfPoint3f();

    Size imageSize;
    Size patternSize;

    // Configure the optimizations used while using openCV's find corners algorithm
    // Since we return results in real-time, we want ensure it goes as fast as possible
    // and fails as fast as possible.
    final int findChessboardFlags =
            Calib3d.CALIB_CB_NORMALIZE_IMAGE
                    | Calib3d.CALIB_CB_ADAPTIVE_THRESH
                    | Calib3d.CALIB_CB_FILTER_QUADS
                    | Calib3d.CALIB_CB_FAST_CHECK;

    private MatOfPoint2f boardCorners = new MatOfPoint2f();

    // Intermedeate result mat's
    Mat smallerInFrame = new Mat();
    MatOfPoint2f smallerBoardCorners = new MatOfPoint2f();

    // SubCornerPix params
    private final Size zeroZone = new Size(-1, -1);
    private final TermCriteria criteria = new TermCriteria(3, 30, 0.001);

    private FindCornersPipeParams lastParams = null;

    public void createObjectPoints() {
        if (this.lastParams != null && this.lastParams.equals(this.params)) return;
        this.lastParams = this.params;

        this.objectPoints.release();
        this.objectPoints = null;
        this.objectPoints = new MatOfPoint3f();

        /*If using a chessboard, then the pattern size if the inner corners of the board. For example, the pattern size of a 9x9 chessboard would be 8x8
        If using a dot board, then the pattern size width is the sum of the bottom 2 rows and the height is the left or right most column
        For example, a 5x4 dot board would have a pattern size of 11x4
        We subtract 1 for chessboard because the UI prompts users for the number of squares, not the
        number of corners.
        * */
        this.patternSize =
                params.type == UICalibrationData.BoardType.CHESSBOARD
                        ? new Size(params.boardWidth - 1, params.boardHeight - 1)
                        : new Size(params.boardWidth, params.boardHeight);

        // Chessboard and dot board have different 3D points to project as a dot board has alternating
        // dots per column
        if (params.type == UICalibrationData.BoardType.CHESSBOARD) {
            // Here we can create an NxN grid since a chessboard is rectangular
            for (int heightIdx = 0; heightIdx < patternSize.height; heightIdx++) {
                for (int widthIdx = 0; widthIdx < patternSize.width; widthIdx++) {
                    double boardYCoord = heightIdx * params.gridSize;
                    double boardXCoord = widthIdx * params.gridSize;
                    objectPoints.push_back(new MatOfPoint3f(new Point3(boardXCoord, boardYCoord, 0.0)));
                }
            }
        } else if (params.type == UICalibrationData.BoardType.DOTBOARD) {
            // Here we need to alternate the amount of dots per column since a dot board is not
            // rectangular and also by taking in account the grid size which should be in mm
            for (int i = 0; i < patternSize.height; i++) {
                for (int j = 0; j < patternSize.width; j++) {
                    objectPoints.push_back(
                            new MatOfPoint3f(
                                    new Point3((2 * j + i % 2) * params.gridSize, i * params.gridSize, 0.0d)));
                }
            }
        } else {
            logger.error("Can't create pattern for unknown board type " + params.type);
        }
    }

    /**
     * Finds the corners in a given image and returns them
     *
     * @param in Input for pipe processing. Pair of input and output mat
     * @return All valid Mats for camera calibration
     */
    @Override
    protected Triple<Size, Mat, Mat> process(Pair<Mat, Mat> in) {
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
        return 1.0 / params.divisor.value;
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
            double deltaX = startPoint[0] - endPoint[0];
            double deltaY = startPoint[1] - endPoint[1];
            double distToNext = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

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
        int width = in.cols() / params.divisor.value;
        int height = in.rows() / params.divisor.value;
        return new Size(width, height);
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
        if (params.type == UICalibrationData.BoardType.CHESSBOARD) {
            // Chessboard uses a dynamic sized window based on how far apart the corners are
            windowHalfWidth = Math.floor(getApproxMinSpacing(inPoints) * 0.50);
            windowHalfWidth = Math.max(1, windowHalfWidth);
        }
        return new Size(windowHalfWidth, windowHalfWidth);
    }

    /**
     * Find chessboard corners given a input mat and output mat to draw on
     *
     * @return Frame resolution, object points, board corners
     */
    private Triple<Size, Mat, Mat> findBoardCorners(Pair<Mat, Mat> in) {
        createObjectPoints();

        var inFrame = in.getLeft();
        var outFrame = in.getRight();

        // Convert the inFrame to grayscale to increase contrast
        Imgproc.cvtColor(inFrame, inFrame, Imgproc.COLOR_BGR2GRAY);
        boolean boardFound = false;

        if (params.type == UICalibrationData.BoardType.CHESSBOARD) {
            // Reduce the image size to be much more manageable
            if (params.divisor != FrameDivisor.NONE) {
                Imgproc.resize(inFrame, smallerInFrame, getFindCornersImgSize(inFrame));
            } else {
                smallerInFrame = inFrame;
            }

            if (params.useMrginghamDetector) {
                int gridn = (int) patternSize.width; // TODO
                var ret = MrginghamJNI.detectChessboard(smallerInFrame, true, 1, true, gridn);
                boardFound = (ret.length == gridn * gridn);
                smallerBoardCorners.fromList(
                        Arrays.stream(ret).map(it -> new Point(it.x, it.y)).collect(Collectors.toList()));
            } else {
                // Run the chessboard corner finder on the smaller image
                boardFound =
                        Calib3d.findChessboardCorners(
                                smallerInFrame, patternSize, smallerBoardCorners, findChessboardFlags);
            }

            // Rescale back to original pixel locations
            if (boardFound) {
                rescalePointsToOrigFrame(smallerBoardCorners, inFrame, boardCorners);
            }

        } else if (params.type == UICalibrationData.BoardType.DOTBOARD) {
            boardFound =
                    Calib3d.findCirclesGrid(
                            inFrame, patternSize, boardCorners, Calib3d.CALIB_CB_ASYMMETRIC_GRID);
        }

        if (!boardFound) {
            // If we can't find a chessboard/dot board, give up
            return null;
        }

        var outBoardCorners = new MatOfPoint2f();
        boardCorners.copyTo(outBoardCorners);

        var objPts = new MatOfPoint2f();
        objectPoints.copyTo(objPts);

        // Get the size of the inFrame
        this.imageSize = new Size(inFrame.width(), inFrame.height());

        // Do sub corner pix for drawing chessboard when using OpenCV
        if (!params.useMrginghamDetector || params.divisor != FrameDivisor.NONE) {
            Imgproc.cornerSubPix(
                    inFrame, outBoardCorners, getWindowSize(outBoardCorners), zeroZone, criteria);
        }

        // draw the chessboard, doesn't have to be different for a dot board since it just re projects
        // the corners we found
        Calib3d.drawChessboardCorners(outFrame, patternSize, outBoardCorners, true);

        return Triple.of(inFrame.size(), objPts, outBoardCorners);
    }

    public static class FindCornersPipeParams {
        final int boardHeight;
        final int boardWidth;
        final UICalibrationData.BoardType type;
        final double gridSize;
        final FrameDivisor divisor;
        private final boolean useMrginghamDetector;

        public FindCornersPipeParams(
                int boardHeight,
                int boardWidth,
                UICalibrationData.BoardType type,
                double gridSize,
                FrameDivisor divisor) {
            this.boardHeight = boardHeight;
            this.boardWidth = boardWidth;
            this.type = type;
            this.gridSize = gridSize; // mm
            this.divisor = divisor;
            this.useMrginghamDetector = MrginghamJNI.isWorking();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + boardHeight;
            result = prime * result + boardWidth;
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            long temp;
            temp = Double.doubleToLongBits(gridSize);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            result = prime * result + ((divisor == null) ? 0 : divisor.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            FindCornersPipeParams other = (FindCornersPipeParams) obj;
            if (boardHeight != other.boardHeight) return false;
            if (boardWidth != other.boardWidth) return false;
            if (type != other.type) return false;
            if (Double.doubleToLongBits(gridSize) != Double.doubleToLongBits(other.gridSize))
                return false;
            if (divisor != other.divisor) return false;
            return true;
        }
    }
}
