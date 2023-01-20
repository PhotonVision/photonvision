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

import java.util.Map;

public class UICalibrationData {
    public final int videoModeIndex;
    public int count;
    public final int minCount;
    public final boolean hasEnough;
    public final double squareSizeIn;
    public final int patternWidth;
    public final int patternHeight;
    public final BoardType boardType; //

    public UICalibrationData(
            int count,
            int videoModeIndex,
            int minCount,
            boolean hasEnough,
            double squareSizeIn,
            int patternWidth,
            int patternHeight,
            BoardType boardType) {
        this.count = count;
        this.minCount = minCount;
        this.videoModeIndex = videoModeIndex;
        this.hasEnough = hasEnough;
        this.squareSizeIn = squareSizeIn;
        this.patternWidth = patternWidth;
        this.patternHeight = patternHeight;
        this.boardType = boardType;
    }

    public enum BoardType {
        CHESSBOARD,
        DOTBOARD
    }

    public static UICalibrationData fromMap(Map<String, Object> map) {
        return new UICalibrationData(
                ((Number) map.get("count")).intValue(),
                ((Number) map.get("videoModeIndex")).intValue(),
                ((Number) map.get("minCount")).intValue(),
                (boolean) map.get("hasEnough"),
                ((Number) map.get("squareSizeIn")).doubleValue(),
                ((Number) map.get("patternWidth")).intValue(),
                ((Number) map.get("patternHeight")).intValue(),
                BoardType.values()[(int) map.get("boardType")]
                );
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
                + ", patternWidth="
                + patternWidth
                + ", patternHeight="
                + patternHeight
                + ", boardType="
                + boardType
                + '}';
    }
}
