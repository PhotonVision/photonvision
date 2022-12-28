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

package org.photonvision.common.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.camera.CameraType;
import org.photonvision.vision.pipeline.CVPipelineSettings;
import org.photonvision.vision.pipeline.DriverModePipelineSettings;
import org.photonvision.vision.processes.PipelineManager;

public class CameraConfiguration {
    private static final Logger logger = new Logger(CameraConfiguration.class, LogGroup.Camera);

    /** Name as reported by CSCore */
    public String baseName = "";

    /** Name used to title the subfolder of this config */
    public String uniqueName = "";

    /** User-set nickname */
    public String nickname = "";

    /** Can be either path (ex /dev/videoX) or index (ex 1). */
    public String path = "";

    @JsonIgnore public String[] otherPaths = {};

    public CameraType cameraType = CameraType.UsbCamera;
    public double FOV = 70;
    public final List<CameraCalibrationCoefficients> calibrations;
    public int currentPipelineIndex = 0;

    public int streamIndex = 0; // 0 index means ports [1181, 1182], 1 means [1183, 1184], etc...

    @JsonIgnore // this ignores the pipes as we serialize them to their own subfolder
    public List<CVPipelineSettings> pipelineSettings = new ArrayList<>();

    @JsonIgnore
    public DriverModePipelineSettings driveModeSettings = new DriverModePipelineSettings();

    public CameraConfiguration(String baseName, String path) {
        this(baseName, baseName, baseName, path, new String[0]);
    }

    public CameraConfiguration(
            String baseName, String uniqueName, String nickname, String path, String[] alternates) {
        this.baseName = baseName;
        this.uniqueName = uniqueName;
        this.nickname = nickname;
        this.path = path;
        this.calibrations = new ArrayList<>();
        this.otherPaths = alternates;

        logger.debug(
                "Creating USB camera configuration for "
                        + cameraType
                        + " "
                        + baseName
                        + " (AKA "
                        + nickname
                        + ") at "
                        + path);
    }

    @JsonCreator
    public CameraConfiguration(
            @JsonProperty("baseName") String baseName,
            @JsonProperty("uniqueName") String uniqueName,
            @JsonProperty("nickname") String nickname,
            @JsonProperty("FOV") double FOV,
            @JsonProperty("path") String path,
            @JsonProperty("cameraType") CameraType cameraType,
            @JsonProperty("calibration") List<CameraCalibrationCoefficients> calibrations,
            @JsonProperty("currentPipelineIndex") int currentPipelineIndex) {
        this.baseName = baseName;
        this.uniqueName = uniqueName;
        this.nickname = nickname;
        this.FOV = FOV;
        this.path = path;
        this.cameraType = cameraType;
        this.calibrations = calibrations != null ? calibrations : new ArrayList<>();
        this.currentPipelineIndex = currentPipelineIndex;

        logger.debug(
                "Creating camera configuration for "
                        + cameraType
                        + " "
                        + baseName
                        + " (AKA "
                        + nickname
                        + ") at "
                        + path);
    }

    public void addPipelineSettings(List<CVPipelineSettings> settings) {
        for (var setting : settings) {
            addPipelineSetting(setting);
        }
    }

    public void addPipelineSetting(CVPipelineSettings setting) {
        if (pipelineSettings.stream()
                .anyMatch(s -> s.pipelineNickname.equalsIgnoreCase(setting.pipelineNickname))) {
            logger.error("Could not name two pipelines the same thing! Renaming");
            setting.pipelineNickname += "_1"; // TODO verify this logic
        }

        if (pipelineSettings.stream().anyMatch(s -> s.pipelineIndex == setting.pipelineIndex)) {
            var newIndex = pipelineSettings.size();
            logger.error("Could not insert two pipelines at same index! Changing to " + newIndex);
            setting.pipelineIndex = newIndex; // TODO verify this logic
        }

        pipelineSettings.add(setting);
        pipelineSettings.sort(PipelineManager.PipelineSettingsIndexComparator);
    }

    public void setPipelineSettings(List<CVPipelineSettings> settings) {
        pipelineSettings = settings;
    }

    public void addCalibration(CameraCalibrationCoefficients calibration) {
        logger.info("adding calibration " + calibration.resolution);
        calibrations.stream()
                .filter(it -> it.resolution.equals(calibration.resolution))
                .findAny()
                .ifPresent(calibrations::remove);
        calibrations.add(calibration);
    }

    @Override
    public String toString() {
        return "CameraConfiguration [baseName="
                + baseName
                + ", uniqueName="
                + uniqueName
                + ", nickname="
                + nickname
                + ", path="
                + path
                + ", otherPaths="
                + Arrays.toString(otherPaths)
                + ", cameraType="
                + cameraType
                + ", FOV="
                + FOV
                + ", calibrations="
                + calibrations
                + ", currentPipelineIndex="
                + currentPipelineIndex
                + ", streamIndex="
                + streamIndex
                + ", pipelineSettings="
                + pipelineSettings
                + ", driveModeSettings="
                + driveModeSettings
                + "]";
    }
}
