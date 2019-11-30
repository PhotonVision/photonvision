package com.chameleonvision.vision.pipeline;

import com.chameleonvision.vision.enums.ImageFlipMode;
import com.chameleonvision.vision.enums.ImageRotationMode;

@SuppressWarnings("ALL")
public class CVPipelineSettings {
    public int index = 0;
    public ImageFlipMode flipMode = ImageFlipMode.NONE;
    public ImageRotationMode rotationMode = ImageRotationMode.DEG_0;
    public String nickname = "New Pipeline";
    public double exposure = 50.0;
    public double brightness = 50.0;
}
