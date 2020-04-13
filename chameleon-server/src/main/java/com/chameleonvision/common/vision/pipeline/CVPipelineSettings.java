package com.chameleonvision.common.vision.pipeline;

import com.chameleonvision.common.vision.frame.FrameDivisor;
import com.chameleonvision.common.vision.pipe.ImageFlipMode;
import com.chameleonvision.common.vision.pipe.ImageRotationMode;

public class CVPipelineSettings {
    public int pipelineIndex = 0;
    public PipelineType pipelineType = PipelineType.DriverMode;
    public ImageFlipMode inputImageFlipMode = ImageFlipMode.NONE;
    public ImageRotationMode inputImageRotationMode = ImageRotationMode.DEG_0;
    public String pipelineNickname = "New Pipeline";
    public double cameraExposure = 50.0;
    public double cameraBrightness = 50.0;
    public double cameraGain = 50.0;
    public int cameraVideoModeIndex = 0;
    public FrameDivisor inputFrameDivisor = FrameDivisor.NONE;
    public FrameDivisor outputFrameDivisor = FrameDivisor.NONE;
}
