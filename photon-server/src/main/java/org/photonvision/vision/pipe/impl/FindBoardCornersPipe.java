/*
 * Copyright (C) 2020 Photon Vision.
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

import java.util.Objects;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
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

    // Tune to taste for a reasonable tradeoff between making
    // the findCorners portion work hard, versus the subpixel refinement work hard.
    final int FIND_CORNERS_WIDTH_PX = 320;

    // Configure the optimizations used while using openCV's find corners algorithm
    // Since we return results in real-time, we want ensure it goes as fast as possible
    // and fails as fast as possible.
    final int findChessboardFlags =
            Calib3d.CALIB_CB_NORMALIZE_IMAGE
                    | Calib3d.CALIB_CB_ADAPTIVE_THRESH
                    | Calib3d.CALIB_CB_FILTER_QUADS
                    | Calib3d.CALIB_CB_FAST_CHECK;

    private MatOfPoint2f boardCorners = new MatOfPoint2f();

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
                for (int widthIdx = 0; widthIdx < patternSize.height; widthIdx++) {
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

        // Create the object points
        createObjectPoints();

        return findBoardCorners(in);
    }

    /**
    * Figures out how much a frame or point cloud must be scaled down by to match the desired size at
    * which to run FindCorners
    *
    * @param inFrame
    * @return
    */
    private double getFindCornersScaleFactor(Mat inFrame) {
        if (inFrame.width() > FIND_CORNERS_WIDTH_PX) {
            return ((double) FIND_CORNERS_WIDTH_PX) / inFrame.width();
        } else {
            return 1.0;
        }
    }

    /**
    * Finds the minimum spacing between a set of x/y points Currently only considers points whose
    * index is next to each other Which, currently, means it traverses one dimension. This is a rough
    * heuristic approach which could be refined in the future.
    *
    * @param inPoints point set to analyze
    * @return min spacing between neighbors
    */
    private double getMinSpacing(MatOfPoint2f inPoints) {
        double minSpacing = Double.MAX_VALUE;
        Point[] inPointsArr = inPoints.toArray();
        for (int idx = 0; idx < inPointsArr.length - 1; idx++) {
            // Heurestic to find the tightest spacing present in the grid
            // Only looks in 1 dimension for now
            double deltaX = inPointsArr[idx + 1].x - inPointsArr[idx].x;
            double deltaY = inPointsArr[idx + 1].y - inPointsArr[idx].y;
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
    private Size getFindCornersImgSize(Mat inFrame) {
        var findcorners_height = Math.round(inFrame.height() * getFindCornersScaleFactor(inFrame));
        return new Size(FIND_CORNERS_WIDTH_PX, findcorners_height);
    }

    /**
    * Given an input frame and a set of points from the "smaller" findChessboardCorner analysis,
    * re-scale the points back to where they would have been in the input frame
    *
    * @param inPoints set of points derived from a call to findChessboardCorner on a shrunken mat
    * @param origFrame Original frame we're rescaling points back to
    * @param outPoints mat into which the output rescaled points get placed
    */
    private void rescalePointsToOrigFrame(
            MatOfPoint2f inPoints, Mat origFrame, MatOfPoint2f outPoints) {
        // Rescale boardCorners back up to the inproc image size
        double sf = getFindCornersScaleFactor(origFrame);
        Point[] inPointsArr = inPoints.toArray();
        Point[] retPointsArr = new Point[inPointsArr.length];
        for (int idx = 0; idx < inPointsArr.length; idx++) {
            double xCoord = inPointsArr[idx].x / sf;
            double yCoord = inPointsArr[idx].y / sf;
            retPointsArr[idx] = new Point(xCoord, yCoord);
        }
        outPoints.fromArray(retPointsArr);
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
            windowHalfWidth = Math.floor(getMinSpacing(inPoints) * 0.50);
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
            // This is for chessboards

            // Reduce the image size to be much more manageable
            Mat smallerInFrame = new Mat();
            Imgproc.resize(inFrame, smallerInFrame, getFindCornersImgSize(inFrame));

            MatOfPoint2f smallerBoardCorners = new MatOfPoint2f();
            // Run the chessboard corner finder on the smaller image
            boardFound =
                    Calib3d.findChessboardCorners(
                            smallerInFrame, patternSize, smallerBoardCorners, findChessboardFlags);

            // Rescale back to original pixel locations
            if (boardFound) {
                rescalePointsToOrigFrame(smallerBoardCorners, inFrame, boardCorners);
            }

            smallerInFrame.release();
            smallerBoardCorners.release();

        } else if (params.type == UICalibrationData.BoardType.DOTBOARD) {
            // For dot boards
            boardFound =
                    Calib3d.findCirclesGrid(
                            inFrame, patternSize, boardCorners, Calib3d.CALIB_CB_ASYMMETRIC_GRID);
        }

        if (!boardFound) {
            // If we can't find a chessboard/dot board, just return
            return null;
        }

        var outBoardCorners = new MatOfPoint2f();
        boardCorners.copyTo(outBoardCorners);

        var objPts = new MatOfPoint2f();
        objectPoints.copyTo(objPts);

        // Get the size of the inFrame
        this.imageSize = new Size(inFrame.width(), inFrame.height());

        // Do sub corner pix for drawing chessboard
        Imgproc.cornerSubPix(
                inFrame, outBoardCorners, getWindowSize(outBoardCorners), zeroZone, criteria);

        // convert back to BGR
        //        Imgproc.cvtColor(inFrame, inFrame, Imgproc.COLOR_GRAY2BGR);
        // draw the chessboard, doesn't have to be different for a dot board since it just re projects
        // the corners we found
        Calib3d.drawChessboardCorners(outFrame, patternSize, outBoardCorners, true);

        //        // Add the 3D points and the points of the corners found
        //        if (addToSnapList) {
        //            this.listOfObjectPoints.add(objectPoints);
        //            this.listOfImagePoints.add(boardCorners);
        //        }

        return Triple.of(inFrame.size(), objPts, outBoardCorners);
    }

    public static class FindCornersPipeParams {

        private final int boardHeight;
        private final int boardWidth;
        private final UICalibrationData.BoardType type;
        private final double gridSize;

        public FindCornersPipeParams(
                int boardHeight, int boardWidth, UICalibrationData.BoardType type, double gridSize) {
            this.boardHeight = boardHeight;
            this.boardWidth = boardWidth;
            this.type = type;
            this.gridSize = gridSize; // mm
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FindCornersPipeParams that = (FindCornersPipeParams) o;
            return boardHeight == that.boardHeight
                    && boardWidth == that.boardWidth
                    && Double.compare(that.gridSize, gridSize) == 0
                    && type == that.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(boardHeight, boardWidth, type, gridSize);
        }
    }
}
