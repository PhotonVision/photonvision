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
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CharucoBoard;
import org.opencv.objdetect.CharucoDetector;
import org.opencv.objdetect.Objdetect;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.pipe.CVPipe;
import org.photonvision.vision.pipeline.UICalibrationData;

public class FindCharucoCornersPipe
        extends CVPipe<
                Pair<Mat, Mat>,
                FindBoardCornersPipe.FindBoardCornersPipeResult,
                FindBoardCornersPipe.FindCornersPipeParams> {
    private static final Logger logger =
            new Logger(FindBoardCornersPipe.class, LogGroup.VisionModule);

    MatOfPoint3f objectPoints = new MatOfPoint3f();

    Size imageSize;
    Size patternSize;

    CharucoBoard board;
    CharucoDetector detector;

    // Intermediate result mat's
    Mat smallerInFrame = new Mat();
    MatOfPoint2f smallerBoardCorners = new MatOfPoint2f();

    private FindBoardCornersPipe.FindCornersPipeParams lastParams = null;

    public void createObjectPoints() {
        if (this.lastParams != null && this.lastParams.equals(this.params)) return;
        this.lastParams = this.params;

        if (params.type == UICalibrationData.BoardType.CHARUCOBOARD) {
            board =
                    new CharucoBoard(
                            new Size(params.boardWidth, params.boardHeight),
                            (float) params.gridSize,
                            (float) params.markerSize,
                            Objdetect.getPredefinedDictionary(params.tagFamily));
            detector = new CharucoDetector(board);
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
    protected FindBoardCornersPipe.FindBoardCornersPipeResult process(Pair<Mat, Mat> in) {
        return findBoardCorners(in);
    }

    /**
     * Find chessboard corners given an input mat and output mat to draw on
     *
     * @return Frame resolution, object points, board corners
     */
    private FindBoardCornersPipe.FindBoardCornersPipeResult findBoardCorners(Pair<Mat, Mat> in) {
        Mat ObjPoints =
                new Mat(); // 3 dimensional currentObjectPoints, the physical target ChArUco Board
        Mat imgPoints =
                new Mat(); // 2 dimensional currentImagePoints, the likely distorted board on the flat
        // camera sensor frame posed relative to the target
        Mat detectedCorners = new Mat(); // currentCharucoCorners
        Mat detectedIds = new Mat(); // currentCharucoIds
        Mat markerIds = new Mat();
        List<Mat> markerCorners = new ArrayList<>();

        createObjectPoints();

        var inFrame = in.getLeft();
        var outFrame = in.getRight();

        // Convert the inFrame too grayscale to increase contrast
        Imgproc.cvtColor(inFrame, inFrame, Imgproc.COLOR_BGR2GRAY);
        boolean boardFound = false;

        if (params.type == UICalibrationData.BoardType.CHARUCOBOARD) {
            detector.detectBoard(inFrame, detectedCorners, detectedIds, markerCorners, markerIds);

            // reformat the Mat to a List<Mat> for matchImagePoints
            final List<Mat> detectedCornersList = new ArrayList<>();
            for (int i = 0; i < detectedCorners.total(); i++) {
                detectedCornersList.add(detectedCorners.row(i));
            }
            if (detectedCornersList.size() > 0) {
                boardFound = true;
            }

            if (!boardFound) {
                // If we can't find a board, give up
                return null;
            }
            board.matchImagePoints(detectedCornersList, detectedIds, ObjPoints, imgPoints);

            // draw the charuco board
            Objdetect.drawDetectedCornersCharuco(
                    outFrame, detectedCorners, detectedIds, new Scalar(0, 0, 255)); // Red
            // Text

        }

        if (!boardFound) {
            // If we can't find a charuco board, give up
            return null;
        }

        var outBoardCorners = new MatOfPoint2f();
        imgPoints.copyTo(outBoardCorners);

        var objPts = new MatOfPoint3f();
        ObjPoints.copyTo(objPts);

        var outLevels = new MatOfFloat();

        if (params.useMrCal) {
            Point[] boardCorners =
                    new Point[(this.params.boardHeight - 1) * (this.params.boardWidth - 1)];
            Point3[] objectPoints =
                    new Point3[(this.params.boardHeight - 1) * (this.params.boardWidth - 1)];
            float[] levels = new float[(this.params.boardHeight - 1) * (this.params.boardWidth - 1)];

            for (int i = 0; i < detectedIds.total(); i++) {
                int id = (int) detectedIds.get(i, 0)[0];
                boardCorners[id] = outBoardCorners.toList().get(i);
                objectPoints[id] = objPts.toList().get(i);
                levels[i] = 1.0f;
            }
            for (int i = 0; i < boardCorners.length; i++) {
                if (boardCorners[i] == null) {
                    boardCorners[i] = new Point(-1, -1);
                    objectPoints[i] = new Point3(-1, -1, -1);
                    levels[i] = -1.0f;
                }
            }

            outBoardCorners.fromArray(boardCorners);
            outLevels.fromArray(levels);
        }

        // Get the size of the inFrame
        this.imageSize = new Size(inFrame.width(), inFrame.height());

        return new FindBoardCornersPipe.FindBoardCornersPipeResult(
                inFrame.size(), objPts, outBoardCorners, outLevels);
    }
}
