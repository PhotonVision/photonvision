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
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.vision.calibration.UICameraCalibrationCoefficients;
import org.photonvision.vision.camera.PVCameraInfo;
import org.photonvision.vision.camera.QuirkyCamera;

public class UICameraConfiguration {
    // Path to the camera device. On Linux, this is a special file in /dev/v4l/by-id
    // or /dev/videoN.
    // This is the path we hand to CSCore to do auto-reconnect on
    public String cameraPath;

    /** See {@link CameraConfiguration #deactivated} */
    public boolean deactivated;

    public String nickname;
    public String uniqueName;

    public double fov;
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
    public PVCameraInfo matchedCameraInfo;

    // Status for if the underlying device is present and such
    public boolean isConnected;
    public boolean hasConnected;
}
