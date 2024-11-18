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
