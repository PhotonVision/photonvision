package com.chameleonvision.common.configuration;

import com.chameleonvision.common.calibration.CameraCalibrationCoefficients;
import com.chameleonvision.common.logging.LogGroup;
import com.chameleonvision.common.logging.Logger;
import com.chameleonvision.common.vision.pipeline.CVPipelineSettings;
import com.chameleonvision.common.vision.pipeline.DriverModePipelineSettings;
import com.chameleonvision.common.vision.processes.PipelineManager;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;

public class CameraConfiguration {
    private static final Logger logger = new Logger(CameraConfiguration.class, LogGroup.Camera);

    public String name = "";
    public String nickname = "";
    public double FOV = 70;
    public CameraCalibrationCoefficients calibration;

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
