package com.chameleonvision.vision;

import java.util.Arrays;
import java.util.List;

public class Pipeline {
	public int exposure = 50;
	public int brightness = 50;
	public String orientation = "Normal";
	public List<Integer> hue = Arrays.asList(50, 180);
	public List<Integer> saturation = Arrays.asList(50, 255);
	public List<Integer> value = Arrays.asList(50, 255);
	public boolean erode = false;
	public boolean dilate = false;
	public List<Double> area = Arrays.asList(0.0, 100.0);
	public List<Double> ratio = Arrays.asList(0.0, 20.0);
	public List<Double> extent = Arrays.asList(0.0, 100.0);
	public int is_binary = 0;
	public String sort_mode = "Largest";
	public String target_group = "Single";
	public String target_intersection = "Up";
	public double M = 1;
	public double B = 0;
	public boolean is_calibrated = false;
}
