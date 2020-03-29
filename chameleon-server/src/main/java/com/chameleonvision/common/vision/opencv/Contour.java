package com.chameleonvision.common.vision.opencv;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

public class Contour {

	private final MatOfPoint points;

	private Double area = Double.NaN;
	private RotatedRect minAreaRect = null;
	private Rect boundingRect = null;

	public Contour(MatOfPoint points) {
		this.points = points;
	}

	public double getArea() {
		if (Double.isNaN(area)) {
			area = Imgproc.contourArea(points);
		}
		return area;
	}

	public RotatedRect getMinAreaRect() {
		if (minAreaRect == null) {
			MatOfPoint2f temp = new MatOfPoint2f(points.toArray());
			minAreaRect = Imgproc.minAreaRect(temp);
			temp.release();
		}
		return minAreaRect;
	}

	public Rect getBoundingRect() {
		if (boundingRect == null) {
			boundingRect = Imgproc.boundingRect(points);
		}
		return boundingRect;
	}

	public Point getCenterPoint() {
		return getMinAreaRect().center;
	}
}
