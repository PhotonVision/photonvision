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

package org.photonvision.common.dataflow.websocket;

import java.util.HashMap;
import java.util.List;
import org.photonvision.vision.calibration.UICameraCalibrationCoefficients;
import org.photonvision.vision.camera.QuirkyCamera;

public class UICameraConfiguration {
    @SuppressWarnings("unused")
    public double fov;

    public String nickname;
    public String uniqueName;
    public HashMap<String, Object> currentPipelineSettings;
    public int currentPipelineIndex;
    public List<String> pipelineNicknames;
    public HashMap<Integer, HashMap<String, Object>> videoFormatList;
    public int outputStreamPort;
    public int inputStreamPort;
    public List<UICameraCalibrationCoefficients> calibrations;
    public boolean isFovConfigurable = true;
    public QuirkyCamera cameraQuirks;
    public boolean isCSICamera;
    public double minExposureRaw;
    public double maxExposureRaw;
    public double minWhiteBalanceTemp;
    public double maxWhiteBalanceTemp;
}
