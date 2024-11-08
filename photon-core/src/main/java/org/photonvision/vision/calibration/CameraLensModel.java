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

package org.photonvision.vision.calibration;

/**
 * What kind of camera lens model our intrinsics are modeling. For more info see:
 * https://docs.opencv.org/4.x/dc/dbb/tutorial_py_calibration.html
 * https://mrcal.secretsauce.net/lensmodels.html#org4e95788
 */
public enum CameraLensModel {
    /** OpenCV[4,5,8,12]-based model */
    LENSMODEL_OPENCV,
    /** Mrcal steriographic lens model. See LENSMODEL_STEREOGRAPHIC in the mrcal docs */
    LENSMODEL_STERIOGRAPHIC,
    /**
     * Mrcal splined-steriographic lens model. See LENSMODEL_SPLINED_STEREOGRAPHIC in the mrcal docs
     */
    LENSMODEL_SPLINED_STERIOGRAPHIC
}
