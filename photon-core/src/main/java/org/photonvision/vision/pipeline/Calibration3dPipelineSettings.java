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

import edu.wpi.first.math.util.Units;
import org.opencv.core.Size;
import org.photonvision.vision.frame.FrameDivisor;

public class Calibration3dPipelineSettings extends AdvancedPipelineSettings {
    public int boardHeight = 8;
    public int boardWidth = 8;
    public UICalibrationData.BoardType boardType = UICalibrationData.BoardType.CHESSBOARD;
    public double gridSize = Units.inchesToMeters(1.0);

    public Size resolution = new Size(640, 480);

    public Calibration3dPipelineSettings() {
        super();
        this.cameraAutoExposure = true;
        this.inputShouldShow = true;
        this.outputShouldShow = true;
        this.streamingFrameDivisor = FrameDivisor.HALF;
    }
}
