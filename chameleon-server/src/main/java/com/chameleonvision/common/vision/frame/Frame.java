package com.chameleonvision.common.vision.frame;

import org.opencv.core.Mat;

public class Frame {
	public long timestampNanos;
	public Mat image;

	public Frame(Mat image) {
		this.image = image;
		timestampNanos = System.nanoTime();
	}

	public Frame(Mat image, long timestampNanos) {
		this.image = image;
		this.timestampNanos = timestampNanos;
	}
}
