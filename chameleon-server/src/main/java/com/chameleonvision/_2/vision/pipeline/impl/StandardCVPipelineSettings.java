package com.chameleonvision._2.vision.pipeline.impl;

import com.chameleonvision._2.vision.enums.*;
import com.chameleonvision._2.vision.pipeline.CVPipelineSettings;
import com.chameleonvision.common.util.numbers.DoubleCouple;
import com.chameleonvision.common.util.numbers.IntegerCouple;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point3;

public class StandardCVPipelineSettings extends CVPipelineSettings {
    public IntegerCouple hue = new IntegerCouple(50, 180);
    public IntegerCouple saturation = new IntegerCouple(50, 255);
    public IntegerCouple value = new IntegerCouple(50, 255);
    public boolean erode = false;
    public boolean dilate = false;
    public DoubleCouple area = new DoubleCouple(0.0, 100.0);
    public DoubleCouple ratio = new DoubleCouple(0.0, 20.0);
    public DoubleCouple extent = new DoubleCouple(0.0, 100.0);
    public Number speckle = 5;
    public boolean isBinary = false;
    public SortMode sortMode = SortMode.Largest;
    public TargetRegion targetRegion = TargetRegion.Center;
    public TargetOrientation targetOrientation = TargetOrientation.Landscape;
    public boolean multiple = false;
    public TargetGroup targetGroup = TargetGroup.Single;
    public TargetIntersection targetIntersection = TargetIntersection.Up;
    public DoubleCouple point = new DoubleCouple();
    public CalibrationMode calibrationMode = CalibrationMode.None;
    public double dualTargetCalibrationM = 1;
    public double dualTargetCalibrationB = 0;

    // 3d stuff
    public MatOfPoint3f targetCornerMat = new MatOfPoint3f();
    public Number accuracy = 5;
    private static MatOfPoint3f hexTargetMat = new MatOfPoint3f(
            new Point3(-19.625, 0, 0),
            new Point3(-9.819867, -17, 0),
            new Point3(9.819867, -17, 0),
            new Point3(19.625, 0, 0)
    );

    public StandardCVPipelineSettings() {
        super();
        hexTargetMat.copyTo(targetCornerMat);
    }


    public boolean is3D = false;
}
