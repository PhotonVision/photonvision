package org.photonvision.common.dataflow.websocket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UIGeneralSettings {
    public UIGeneralSettings(
            String version,
            String gpuAcceleration,
            boolean mrCalWorking,
            Map<String, ArrayList<String>> availableModels,
            List<String> supportedBackends,
            String hardwareModel,
            String hardwarePlatform) {
        this.version = version;
        this.gpuAcceleration = gpuAcceleration;
        this.mrCalWorking = mrCalWorking;
        this.availableModels = availableModels;
        this.supportedBackends = supportedBackends;
        this.hardwareModel = hardwareModel;
        this.hardwarePlatform = hardwarePlatform;
    }

    public String version;
    public String gpuAcceleration;
    public boolean mrCalWorking;
    public Map<String, ArrayList<String>> availableModels;
    public List<String> supportedBackends;
    public String hardwareModel;
    public String hardwarePlatform;
}
