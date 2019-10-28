package com.chameleonvision.vision;

import java.util.Arrays;
import java.util.List;

public class Pipeline {
	public int exposure = 50;
	public int brightness = 50;
	public Orientation orientation = Orientation.Normal;
	public List<Number> hue = Arrays.asList(50, 180);
	public List<Number> saturation = Arrays.asList(50, 255);
	public List<Number> value = Arrays.asList(50, 255);
	public boolean erode = false;
	public boolean dilate = false;
	public List<Number> area = Arrays.asList(0.0, 100.0);
	public List<Number> ratio = Arrays.asList(0.0, 20.0);
	public List<Number> extent = Arrays.asList(0, 100);
	public Number speckle = 5;
	public boolean isBinary = false;
	public SortMode sortMode = SortMode.Largest;
	public TargetGroup targetGroup = TargetGroup.Single;
	public TargetIntersection targetIntersection = TargetIntersection.Up;
	public double m = 1;
	public double b = 0;
	public List<Number> point = Arrays.asList(0,0);
	public CalibrationMode calibrationMode = CalibrationMode.None;
    public String nickname;
}
