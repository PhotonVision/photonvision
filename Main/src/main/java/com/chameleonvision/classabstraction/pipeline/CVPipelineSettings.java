package com.chameleonvision.classabstraction.pipeline;

import com.chameleonvision.vision.*;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("ALL")
public abstract class CVPipelineSettings {
    ImageFlipMode flipMode = ImageFlipMode.NONE;
    List<Number> hue = Arrays.asList(50, 180);
    List<Number> saturation = Arrays.asList(50, 255);
    List<Number> value = Arrays.asList(50, 255);
    boolean erode = false;
    boolean dilate = false;
    List<Number> area = Arrays.asList(0.0, 100.0);
    List<Number> ratio = Arrays.asList(0.0, 20.0);
    List<Number> extent = Arrays.asList(0, 100);
    Number speckle = 5;
    boolean isBinary = false;
    SortMode sortMode = SortMode.Largest;
    TargetGroup targetGroup = TargetGroup.Single;
    TargetIntersection targetIntersection = TargetIntersection.Up;
    List<Number> point = Arrays.asList(0,0);
    CalibrationMode calibrationMode = CalibrationMode.None;

    String nickname = "";
    double exposure = 50.0;
    double brightness = 50.0;
}
