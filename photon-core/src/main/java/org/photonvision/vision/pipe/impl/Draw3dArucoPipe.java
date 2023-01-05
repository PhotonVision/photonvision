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

import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.frame.FrameDivisor;
import org.photonvision.vision.target.TargetModel;

public class Draw3dArucoPipe extends Draw3dTargetsPipe {
    public static class Draw3dArucoParams extends Draw3dContoursParams {
        public Draw3dArucoParams(
                boolean shouldDraw,
                CameraCalibrationCoefficients cameraCalibrationCoefficients,
                TargetModel targetModel,
                FrameDivisor divisor) {
            super(shouldDraw, cameraCalibrationCoefficients, targetModel, divisor);
            this.shouldDrawHull = false;
        }
    }
}
