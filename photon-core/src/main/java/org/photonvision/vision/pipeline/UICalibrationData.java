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

import org.opencv.objdetect.Objdetect;

public record UICalibrationData(
        int count,
        int videoModeIndex,
        int minCount,
        boolean hasEnough,
        double squareSizeIn,
        double markerSizeIn,
        int patternWidth,
        int patternHeight,
        BoardType boardType,
        boolean useOldPattern,
        TagFamily tagFamily) {
    public enum BoardType {
        CHESSBOARD,
        CHARUCOBOARD,
    }

    public enum TagFamily {
        Dict_4X4_1000(Objdetect.DICT_4X4_1000),
        Dict_5X5_1000(Objdetect.DICT_5X5_1000),
        Dict_6X6_1000(Objdetect.DICT_6X6_1000),
        Dict_7X7_1000(Objdetect.DICT_7X7_1000);

        private int value;

        // getter method
        public int getValue() {
            return this.value;
        }

        // enum constructor - cannot be public or protected
        private TagFamily(int value) {
            this.value = value;
        }
    }
}
