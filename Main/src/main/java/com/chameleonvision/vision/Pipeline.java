package com.chameleonvision.vision;

import java.util.Arrays;
import java.util.List;

public class Pipeline {
	public int exposure = 50;
	public int brightness = 50;
	public Orientation orientation = Orientation.Normal;
	public List<Integer> hue = Arrays.asList(50, 180);
	public List<Integer> saturation = Arrays.asList(50, 255);
	public List<Integer> value = Arrays.asList(50, 255);
	public boolean erode = false;
	public boolean dilate = false;
	public List<Float> area = Arrays.asList(0f, 100f);
	public List<Float> ratio = Arrays.asList(0f, 20f);
	public List<Integer> extent = Arrays.asList(0, 100);
	public boolean isBinary = false;
	public SortMode sortMode = SortMode.Largest;
	public TargetGroup targetGroup = TargetGroup.Single;
	public TargetIntersection targetIntersection = TargetIntersection.Up;
	public double M = 1;
	public double B = 0;
	public boolean isCalibrated = false;
}
