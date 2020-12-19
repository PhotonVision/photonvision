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

    private MatOfPoint2f boardCorners = new MatOfPoint2f();

    // SubCornerPix params
    private final Size windowSize = new Size(11, 11);
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
            for (int i = 0; i < patternSize.height * patternSize.width; i++) {
                double boardYCoord =  Math.floor(i / patternSize.width) * params.gridSize;
                double boardXCoord = i % patternSize.width * params.gridSize;
                objectPoints.push_back(
                        new MatOfPoint3f(
                                new Point3(boardXCoord, boardYCoord, 0.0)));
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
            boardFound = Calib3d.findChessboardCorners(inFrame, patternSize, boardCorners);
        } else if (params.type == UICalibrationData.BoardType.DOTBOARD) {
            // For dot boards
            boardFound =
                    Calib3d.findCirclesGrid(
                            inFrame, patternSize, boardCorners, Calib3d.CALIB_CB_ASYMMETRIC_GRID);
        }

        if (!boardFound) {
            // If we can't find a chessboard/dot board, convert the inFrame back to BGR and return false.

            return null;
        }
        var outBoardCorners = new MatOfPoint2f();
        boardCorners.copyTo(outBoardCorners);

        var objPts = new MatOfPoint2f();
        objectPoints.copyTo(objPts);

        // Get the size of the inFrame
        this.imageSize = new Size(inFrame.width(), inFrame.height());

        // Do sub corner pix for drawing chessboard
        Imgproc.cornerSubPix(inFrame, outBoardCorners, windowSize, zeroZone, criteria);

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
