package com.chameleonvision.vision.process;

import org.opencv.core.RotatedRect;

@SuppressWarnings("WeakerAccess")
public class PipelineResult {
	public final boolean IsValid;
	public final double CalibratedX;
	public final double CalibratedY;
	public final double Pitch;
	public final double Yaw;
	public final double Area;
	public final RotatedRect RawPoint;

	public PipelineResult() {
		IsValid = false;
		CalibratedX = 0.0;
		CalibratedY = 0.0;
		Pitch = 0.0;
		Yaw = 0.0;
		Area = 0.0;
		RawPoint = new RotatedRect();
	}

	public PipelineResult(double calX, double calY, double pitch, double yaw, double area, RotatedRect rawPoint) {
		IsValid = true;
		CalibratedX = calX;
		CalibratedY = calY;
		Pitch = pitch;
		Yaw = yaw;
		Area = area;
		RawPoint = rawPoint;
	}
}
