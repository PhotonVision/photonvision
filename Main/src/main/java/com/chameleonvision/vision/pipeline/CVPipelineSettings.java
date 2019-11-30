package com.chameleonvision.vision.pipeline;

import com.chameleonvision.vision.enums.ImageFlipMode;
import com.chameleonvision.vision.enums.ImageRotationMode;
import com.chameleonvision.vision.enums.StreamDivisor;

@SuppressWarnings("ALL")
public class CVPipelineSettings {
    public int index = 0;
    public ImageFlipMode flipMode = ImageFlipMode.NONE;
    public ImageRotationMode rotationMode = ImageRotationMode.DEG_0;
    public String nickname = "New Pipeline";
    public double exposure = 50.0;
    public double brightness = 50.0;
    public double gain = 0;
    public int videoModeIndex = 0;
    public StreamDivisor streamDivisor = StreamDivisor.NONE;
}
