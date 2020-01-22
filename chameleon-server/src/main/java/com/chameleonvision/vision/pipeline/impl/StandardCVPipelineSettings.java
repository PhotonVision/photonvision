package com.chameleonvision.vision.pipeline.impl;

import com.chameleonvision.vision.enums.*;
import com.chameleonvision.vision.pipeline.CVPipelineSettings;
import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.wpi.first.wpilibj.util.Units;
import org.opencv.core.*;

import java.util.Arrays;
import java.util.List;

public class StandardCVPipelineSettings extends CVPipelineSettings {
    public List<Number> hue = Arrays.asList(50, 180);
    public List<Number> saturation = Arrays.asList(50, 255);
    public List<Number> value = Arrays.asList(50, 255);
    public boolean erode = false;
    public boolean dilate = false;
    public List<Number> area = Arrays.asList(0.0, 100.0);
    public List<Number> ratio = Arrays.asList(0.0, 20.0);
    public List<Number> extent = Arrays.asList(0.0, 100.0);
    public Number speckle = 5;
    public boolean isBinary = false;
    public SortMode sortMode = SortMode.Largest;
    public TargetRegion targetRegion = TargetRegion.Center;
    public TargetOrientation targetOrientation = TargetOrientation.Landscape;
    public boolean multiple = false;
    public TargetGroup targetGroup = TargetGroup.Single;
    public TargetIntersection targetIntersection = TargetIntersection.Up;
    public List<Number> point = Arrays.asList(0, 0);
    public CalibrationMode calibrationMode = CalibrationMode.None;
    public double dualTargetCalibrationM = 1;
    public double dualTargetCalibrationB = 0;

    // 3d stuff
    public MatOfPoint3f targetCornerMat = new MatOfPoint3f();
    private static MatOfPoint3f hexTargetMat = new MatOfPoint3f();

    static {
        hexTargetMat.fromList(List.of(
            new Point3(-19.625, 0, 0),
            new Point3(-9.819867, -17, 0),
            new Point3(9.819867, -17, 0),
            new Point3(19.625, 0, 0)));
    }

    public StandardCVPipelineSettings() {
        super();
        hexTargetMat.copyTo(targetCornerMat);
    }


    public boolean is3D = false;
}
