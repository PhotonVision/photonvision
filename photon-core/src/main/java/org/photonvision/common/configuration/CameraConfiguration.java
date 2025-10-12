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
import edu.wpi.first.cscore.UsbCameraInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.photonvision.common.dataflow.websocket.UICameraConfiguration;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.camera.CameraType;
import org.photonvision.vision.camera.PVCameraInfo;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.pipeline.CVPipelineSettings;
import org.photonvision.vision.pipeline.DriverModePipelineSettings;
import org.photonvision.vision.processes.PipelineManager;

public class CameraConfiguration {
    private static final Logger logger = new Logger(CameraConfiguration.class, LogGroup.Camera);

    /** A unique name (ostensibly an opaque UUID) to identify this particular configuration */
    public String uniqueName = "";

    /**
     * The info of the camera we last matched to. We still match by unique path (where we can), but
     * this is useful to provide warnings to users
     */
    public PVCameraInfo matchedCameraInfo;

    /** User-set nickname */
    public String nickname = "";

    /** Deactivated vision modules do not open camera hardware or lock USB ports */
    public boolean deactivated = false;

    public QuirkyCamera cameraQuirks;

    public double FOV = 70;
    public List<CameraCalibrationCoefficients> calibrations = new ArrayList<>();
    public int currentPipelineIndex = 0;

    public int streamIndex = 0; // 0 index means ports [1181, 1182], 1 means [1183, 1184], etc...

    // Ignore the pipes, as we serialize them to their own column to hack around
    // polymorphic lists
    @JsonIgnore public List<CVPipelineSettings> pipelineSettings = new ArrayList<>();

    @JsonIgnore
    public DriverModePipelineSettings driveModeSettings = new DriverModePipelineSettings();

    public CameraConfiguration(PVCameraInfo cameraInfo, String uniqueName, String nickname) {
        this.matchedCameraInfo = cameraInfo;
        this.uniqueName = uniqueName;
        this.nickname = nickname;
        this.calibrations = new ArrayList<>();

        logger.debug("Creating USB camera configuration for " + this.toShortString());
    }

    // Shiny new constructor
    @JsonCreator
    public CameraConfiguration(
            @JsonProperty("uniqueName") String uniqueName,
            @JsonProperty("matchedCameraInfo") PVCameraInfo matchedCameraInfo,
            @JsonProperty("nickname") String nickname,
            @JsonProperty("deactivated") boolean deactivated,
            @JsonProperty("cameraQuirks") QuirkyCamera cameraQuirks,
            @JsonProperty("FOV") double FOV,
            @JsonProperty("calibrations") List<CameraCalibrationCoefficients> calibrations,
            @JsonProperty("currentPipelineIndex") int currentPipelineIndex) {
        this.uniqueName = uniqueName;
        this.matchedCameraInfo = matchedCameraInfo;
        this.nickname = nickname;
        this.deactivated = deactivated;
        this.cameraQuirks = cameraQuirks;
        this.FOV = FOV;
        this.calibrations = calibrations != null ? calibrations : new ArrayList<>();
        this.currentPipelineIndex = currentPipelineIndex;
    }

    // Special case constructor for use with File sources
    public CameraConfiguration(String uniqueName, PVCameraInfo camInfo) {
        this.uniqueName = uniqueName;
        this.matchedCameraInfo = camInfo;
        this.nickname = camInfo.humanReadableName();
        this.calibrations = new ArrayList<>();
        this.cameraQuirks = null; // we'll deal with this later. TODO: should we not just do it now?
    }

    /**
     * Constructor for when we don't know anything about the camera yet. Generates a UUID for the
     * unique name
     */
    public CameraConfiguration(PVCameraInfo camInfo) {
        this(UUID.randomUUID().toString(), camInfo);
    }

    public static class LegacyCameraConfigStruct {
        PVCameraInfo matchedCameraInfo;

        /** Legacy constructor for compat with 2024.3.1 */
        @JsonCreator
        public LegacyCameraConfigStruct(
                @JsonProperty("baseName") String baseName,
                @JsonProperty("path") String path,
                @JsonProperty("otherPaths") String[] otherPaths,
                @JsonProperty("cameraType") CameraType cameraType,
                @JsonProperty("usbVID") int usbVID,
                @JsonProperty("usbPID") int usbPID) {
            if (cameraType == CameraType.UsbCamera) {
                this.matchedCameraInfo =
                        PVCameraInfo.fromUsbCameraInfo(
                                new UsbCameraInfo(-1, path, baseName, otherPaths, usbVID, usbPID));
            } else if (cameraType == CameraType.ZeroCopyPicam) {
                this.matchedCameraInfo = PVCameraInfo.fromCSICameraInfo(path, baseName);
            } else {
                // wtf
                logger.error("Camera type is invalid");
                this.matchedCameraInfo = null;
                return;
            }
        }
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

    /**
     * Replace a calibration in our list with the same unrotatedImageSize with a new one, or add it if
     * none exists yet. If we are replacing an existing calibration, the old one will be "released"
     * and the underlying data matrices will become invalid.
     *
     * @param calibration The calibration to add.
     */
    public void addCalibration(CameraCalibrationCoefficients calibration) {
        logger.info("adding calibration " + calibration.unrotatedImageSize);
        calibrations.stream()
                .filter(it -> it.unrotatedImageSize.equals(calibration.unrotatedImageSize))
                .findAny()
                .ifPresent(
                        (it) -> {
                            it.release();
                            calibrations.remove(it);
                        });
        calibrations.add(calibration);
    }

    /**
     * cscore will auto-reconnect to the camera path we give it. v4l does not guarantee that if i swap
     * cameras around, the same /dev/videoN ID will be assigned to that camera. So instead default to
     * pinning to a particular USB port, or by "path" (appears to be a global identifier on Windows).
     *
     * <p>This represents our best guess at an immutable path to detect a camera at.
     */
    @JsonIgnore
    public String getDevicePath() {
        return matchedCameraInfo.uniquePath();
    }

    public String toShortString() {
        return "CameraConfiguration [uniqueName="
                + uniqueName
                + ", matchedCameraInfo="
                + matchedCameraInfo
                + ", nickname="
                + nickname
                + ", deactivated="
                + deactivated
                + ", cameraQuirks="
                + cameraQuirks
                + ", FOV="
                + FOV
                + "]";
    }

    @Override
    public String toString() {
        return "CameraConfiguration [uniqueName="
                + uniqueName
                + ", matchedCameraInfo="
                + matchedCameraInfo
                + ", nickname="
                + nickname
                + ", deactivated="
                + deactivated
                + ", cameraQuirks="
                + cameraQuirks
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

    /**
     * UICameraConfiguration has some stuff particular to VisionModule, but enough of it's common to
     * warrant this helper
     */
    public UICameraConfiguration toUiConfig() {
        var ret = new UICameraConfiguration();

        ret.matchedCameraInfo = matchedCameraInfo;
        ret.cameraPath = getDevicePath();
        ret.nickname = nickname;
        ret.uniqueName = uniqueName;
        ret.deactivated = deactivated;
        ret.isCSICamera = matchedCameraInfo.type() == CameraType.ZeroCopyPicam;
        ret.pipelineNicknames = pipelineSettings.stream().map(it -> it.pipelineNickname).toList();
        ret.cameraQuirks = cameraQuirks;
        ret.calibrations =
                calibrations.stream().map(CameraCalibrationCoefficients::cloneWithoutObservations).toList();

        return ret;
    }
}
