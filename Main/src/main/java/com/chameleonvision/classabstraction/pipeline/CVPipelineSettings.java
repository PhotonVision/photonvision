package com.chameleonvision.classabstraction.pipeline;

import com.chameleonvision.vision.CalibrationMode;
import com.chameleonvision.vision.SortMode;
import com.chameleonvision.vision.TargetGroup;
import com.chameleonvision.vision.TargetIntersection;

import java.util.Arrays;
import java.util.List;

public abstract class CVPipelineSettings {
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
    double m = 1;
    double b = 0;
    List<Number> point = Arrays.asList(0,0);
    CalibrationMode calibrationMode = CalibrationMode.None;
    String nickname = "";
}
