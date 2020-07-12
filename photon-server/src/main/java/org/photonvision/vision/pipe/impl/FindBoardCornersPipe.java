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

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.photonvision.vision.pipe.CVPipe;

public class FindBoardCornersPipe
        extends CVPipe<List<Mat>, List<List<Mat>>, FindBoardCornersPipe.FindCornersPipeParams> {
    MatOfPoint3f objectPoints = new MatOfPoint3f();
    private final List<Mat> listOfObjectPoints = new ArrayList<>();
    private final List<Mat> listOfImagePoints = new ArrayList<>();

    Size imageSize;
    Size patternSize;

    private MatOfPoint2f boardCorners = new MatOfPoint2f();

    // SubCornerPix params
    private final Size windowSize = new Size(11, 11);
    private final Size zeroZone = new Size(-1, -1);
    private final TermCriteria criteria = new TermCriteria(3, 30, 0.001);

    private boolean objectPointsCreated = false;

    public void createObjectPoints() {
        if (objectPointsCreated) return;

        /*If using a chessboard, then the pattern size if the inner corners of the board. For example, the pattern size of a 9x9 chessboard would be 8x8
        If using a dot board, then the pattern size width is the sum of the bottom 2 rows and the height is the left or right most column
        For example, a 5x4 dot board would have a pattern size of 11x4
        * */
        this.patternSize = new Size(params.boardWidth, params.boardHeight);

        // Chessboard and dot board have different 3D points to project as a dot board has alternating
        // dots per column
        if (params.isUsingChessboard) {
            // Here we can create an NxN grid since a chessboard is rectangular
            for (int i = 0; i < patternSize.height * patternSize.width; i++) {
                objectPoints.push_back(
                        new MatOfPoint3f(
                                new Point3((double) i / patternSize.width, i % patternSize.width, 0.0f)));
            }
        } else {
            // Here we need to alternate the amount of dots per column since a dot board is not
            // rectangular and also by taking in account the grid size which should be in mm
            for (int i = 0; i < patternSize.height; i++) {
                for (int j = 0; j < patternSize.width; j++) {
                    objectPoints.push_back(
                            new MatOfPoint3f(
                                    new Point3((2 * j + i % 2) * params.gridSize, i * params.gridSize, 0.0d)));
                }
            }
        }
        objectPointsCreated = true;
    }

    /**
    * Runs the process for the pipe.
    *
    * @param in Input for pipe processing.
    * @return All valid Mats for camera calibration
    */
    @Override
    protected List<List<Mat>> process(List<Mat> in) {
        // If we have less than 20 snapshots we need to return null
        if (in.size() < 20) return null;
        // Contains all valid Mats where a chessboard or dot board have been found
        List<Mat> outputMats = new ArrayList<>();

        // Create the object points
        createObjectPoints();

        for (Mat board : in) {
            if (findBoardCorners(board).getLeft()) {
                outputMats.add(board);
            }
        }
        // Contains the list of valid Mats, object points and images points where objectPoints.size() =
        // imagePoints.size()
        return List.of(outputMats, listOfObjectPoints, listOfImagePoints);
    }

    public Pair<Boolean, Mat> findBoardCorners(Mat frame) {
        createObjectPoints();

        // Convert the frame to grayscale to increase contrast
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
        boolean boardFound;

        if (params.isUsingChessboard) {
            // This is for chessboards
            boardFound = Calib3d.findChessboardCorners(frame, patternSize, boardCorners);
        } else {
            // For dot boards
            boardFound =
                    Calib3d.findCirclesGrid(
                            frame, patternSize, boardCorners, Calib3d.CALIB_CB_ASYMMETRIC_GRID);
        }

        if (!boardFound) {
            // If we can't find a chessboard/dot board, convert the frame back to BGR and return false.
            Imgproc.cvtColor(frame, frame, Imgproc.COLOR_GRAY2BGR);

            return Pair.of(false, null);
        }
        // Get the size of the frame
        this.imageSize = new Size(frame.width(), frame.height());

        // Add the 3D points and the points of the corners found
        this.listOfObjectPoints.add(objectPoints);
        this.listOfImagePoints.add(boardCorners);

        // Do sub corner pix for drawing chessboard
        Imgproc.cornerSubPix(frame, boardCorners, windowSize, zeroZone, criteria);

        // convert back to BGR
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_GRAY2BGR);
        // draw the chessboard, doesn't have to be different for a dot board since it just re projects
        // the corners we found
        Mat chessboardDrawn = new Mat();
        frame.copyTo(chessboardDrawn);
        Calib3d.drawChessboardCorners(chessboardDrawn, patternSize, boardCorners, true);
        boardCorners = new MatOfPoint2f();
        return Pair.of(true, chessboardDrawn);
    }

    public static class FindCornersPipeParams {

        private final int boardHeight;
        private final int boardWidth;
        private final boolean isUsingChessboard;
        private final double gridSize;

        public FindCornersPipeParams(
                int boardHeight, int boardWidth, boolean isUsingChessboard, double gridSize) {
            this.boardHeight = boardHeight;
            this.boardWidth = boardWidth;
            this.isUsingChessboard = isUsingChessboard;
            this.gridSize = gridSize; // mm
        }
    }
}
