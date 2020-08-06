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

import org.apache.commons.lang3.tuple.Triple;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.photonvision.vision.pipe.CVPipe;
import org.photonvision.vision.pipeline.UICalibrationData;

public class FindBoardCornersPipe
        extends CVPipe<Mat, Triple<Size, Mat, Mat>, FindBoardCornersPipe.FindCornersPipeParams> {
    MatOfPoint3f objectPoints = new MatOfPoint3f();

    Size imageSize;
    Size patternSize;

    private MatOfPoint2f boardCorners = new MatOfPoint2f();

    // SubCornerPix params
    private final Size windowSize = new Size(11, 11);
    private final Size zeroZone = new Size(-1, -1);
    private final TermCriteria criteria = new TermCriteria(3, 30, 0.001);

    private boolean objectPointsCreated = false;

    @Override
    public void setParams(FindCornersPipeParams params) {
        super.setParams(params);

        if (new Size(params.boardWidth, params.boardHeight).equals(patternSize)) return;

        objectPointsCreated = false;
    }

    public void createObjectPoints() {
        if (objectPointsCreated) return; // TODO reinstantiate on settings change

        /*If using a chessboard, then the pattern size if the inner corners of the board. For example, the pattern size of a 9x9 chessboard would be 8x8
        If using a dot board, then the pattern size width is the sum of the bottom 2 rows and the height is the left or right most column
        For example, a 5x4 dot board would have a pattern size of 11x4
        * */
        this.patternSize = new Size(params.boardWidth, params.boardHeight);

        // Chessboard and dot board have different 3D points to project as a dot board has alternating
        // dots per column
        if (params.type == UICalibrationData.BoardType.CHESSBOARD) {
            // Here we can create an NxN grid since a chessboard is rectangular
            for (int i = 0; i < patternSize.height * patternSize.width; i++) {
                objectPoints.push_back(
                        new MatOfPoint3f(
                                new Point3((double) i / patternSize.width * params.gridSize, i % patternSize.width * params.gridSize, 0.0f)));
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
            // TOOD log
        }
        objectPointsCreated = true;
    }

    /**
    * Finds the corners in a given image and returns them
    *
    * @param in Input for pipe processing.
    * @return All valid Mats for camera calibration
    */
    @Override
    protected Triple<Size, Mat, Mat> process(Mat in) {

        // Create the object points
        createObjectPoints();

        return findBoardCorners(in);
    }

    private Triple<Size, Mat, Mat> findBoardCorners(Mat frame) {
        createObjectPoints();

        // Convert the frame to grayscale to increase contrast
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
        boolean boardFound = false;

        if (params.type == UICalibrationData.BoardType.CHESSBOARD) {
            // This is for chessboards
            boardFound = Calib3d.findChessboardCorners(frame, patternSize, boardCorners);
        } else if (params.type == UICalibrationData.BoardType.DOTBOARD) {
            // For dot boards
            boardFound =
                    Calib3d.findCirclesGrid(
                            frame, patternSize, boardCorners, Calib3d.CALIB_CB_ASYMMETRIC_GRID);
        }

        if (!boardFound) {
            // If we can't find a chessboard/dot board, convert the frame back to BGR and return false.
            Imgproc.cvtColor(frame, frame, Imgproc.COLOR_GRAY2BGR);

            return null;
        }
        var outBoardCorners = new MatOfPoint2f();
        boardCorners.copyTo(outBoardCorners);

        // Get the size of the frame
        this.imageSize = new Size(frame.width(), frame.height());

        // Do sub corner pix for drawing chessboard
        Imgproc.cornerSubPix(frame, outBoardCorners, windowSize, zeroZone, criteria);

        // convert back to BGR
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_GRAY2BGR);
        // draw the chessboard, doesn't have to be different for a dot board since it just re projects
        // the corners we found
        Calib3d.drawChessboardCorners(frame, patternSize, outBoardCorners, true);

        //        // Add the 3D points and the points of the corners found
        //        if (addToSnapList) {
        //            this.listOfObjectPoints.add(objectPoints);
        //            this.listOfImagePoints.add(boardCorners);
        //        }

        return Triple.of(frame.size(), objectPoints, outBoardCorners);
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
    }
}
