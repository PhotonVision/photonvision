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

public class UICalibrationData {
    public int videoModeIndex;
    public int count;
    public int minCount;
    public boolean hasEnough;
    public double squareSizeIn;
    public int patternWidth;
    public int patternHeight;
    public BoardType boardType;
    public boolean useMrCal;
    public double markerSizeIn;

    public UICalibrationData() {}

    public UICalibrationData(
            int count,
            int videoModeIndex,
            int minCount,
            boolean hasEnough,
            double squareSizeIn,
            double markerSizeIn,
            int patternWidth,
            int patternHeight,
            BoardType boardType,
            boolean useMrCal) {
        this.count = count;
        this.minCount = minCount;
        this.videoModeIndex = videoModeIndex;
        this.hasEnough = hasEnough;
        this.squareSizeIn = squareSizeIn;
        this.markerSizeIn = markerSizeIn;
        this.patternWidth = patternWidth;
        this.patternHeight = patternHeight;
        this.boardType = boardType;
        this.useMrCal = useMrCal;
    }

    public enum BoardType {
        CHESSBOARD,
        CHARUCOBOARD,
    }

    @Override
    public String toString() {
        return "UICalibrationData{"
                + "videoModeIndex="
                + videoModeIndex
                + ", count="
                + count
                + ", minCount="
                + minCount
                + ", hasEnough="
                + hasEnough
                + ", squareSizeIn="
                + squareSizeIn
                + ", markerSizeIn="
                + markerSizeIn
                + ", patternWidth="
                + patternWidth
                + ", patternHeight="
                + patternHeight
                + ", boardType="
                + boardType
                + '}';
    }
}
