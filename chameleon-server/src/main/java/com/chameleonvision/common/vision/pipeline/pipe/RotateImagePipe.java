package com.chameleonvision.common.vision.pipeline.pipe;

import com.chameleonvision.common.vision.pipeline.CVPipe;
import org.opencv.core.Core;
import org.opencv.core.Mat;

/** Pipe that rotates an image to a given orientation */
public class RotateImagePipe extends CVPipe<Mat, Mat, RotateImagePipe.RotateImageParams> {

	public RotateImagePipe() {
		setParams(RotateImageParams.DEFAULT);
	}

	public RotateImagePipe(RotateImageParams params) {
		setParams(params);
	}

	/**
	* Process this pipe
	*
	* @param in {@link Mat} to be rotated
	* @return Rotated {@link Mat}
	*/
	@Override
	protected Mat process(Mat in) {
		Core.rotate(in, in, params.rotation.value);
		return in;
	}

	public static class RotateImageParams {
		public static RotateImageParams DEFAULT = new RotateImageParams(ImageRotation.DEG_0);

		public ImageRotation rotation;

		public RotateImageParams() {
			rotation = DEFAULT.rotation;
		}

		public RotateImageParams(ImageRotation rotation) {
			this.rotation = rotation;
		}

		public enum ImageRotation {
			DEG_0(-1),
			DEG_90(0),
			DEG_180(1),
			DEG_270(2);

			public final int value;

			ImageRotation(int value) {
				this.value = value;
			}

			public boolean isRotated() {
				return this.value == DEG_90.value || this.value == DEG_270.value;
			}
		}
	}
}
