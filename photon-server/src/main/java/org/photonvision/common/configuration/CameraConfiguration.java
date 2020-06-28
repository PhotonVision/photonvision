package org.photonvision.common.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
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

    public String baseName = "";
    public String uniqueName = "";
    public String nickname = "";
    public double FOV = 70;
    public String path = "";
    public CameraType cameraType = CameraType.UsbCamera;
    public CameraCalibrationCoefficients calibration;
    public List<Integer> CameraLEDs = new ArrayList<>();

    public CameraConfiguration(String baseName, String path) {
        this(baseName, baseName, baseName, path);
    }

    public CameraConfiguration(String baseName, String uniqueName, String nickname, String path) {
        this.baseName = baseName;
        this.uniqueName = uniqueName;
        this.nickname = nickname;
        this.path = path;
    }

    @JsonCreator
    public CameraConfiguration(
            @JsonProperty("baseName") String baseName,
            @JsonProperty("uniqueName") String uniqueName,
            @JsonProperty("nickname") String nickname,
            @JsonProperty("FOV") double FOV,
            @JsonProperty("path") String path,
            @JsonProperty("cameraType") CameraType cameraType,
            @JsonProperty("calibration") CameraCalibrationCoefficients calibration,
            @JsonProperty("CameraLEDs") List<Integer> cameraLEDs) {
        this.baseName = baseName;
        this.uniqueName = uniqueName;
        this.nickname = nickname;
        this.FOV = FOV;
        this.path = path;
        this.cameraType = cameraType;
        this.calibration = calibration;
        this.CameraLEDs = cameraLEDs;
    }

    @JsonIgnore // this ignores the pipes as we serialize them to their own subfolder
    public final List<CVPipelineSettings> pipelineSettings = new ArrayList<>();

    @JsonIgnore
    public DriverModePipelineSettings driveModeSettings = new DriverModePipelineSettings();

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
}
