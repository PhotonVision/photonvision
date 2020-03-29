package com.chameleonvision.common.vision.pipeline.pipe;

import com.chameleonvision.common.vision.pipeline.CVPipe;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/** Pipe that resizes an image to a given resolution */
public class ResizeImagePipe extends CVPipe<Mat, Mat, ResizeImagePipe.ResizeImageParams> {

	public ResizeImagePipe() {
		setParams(ResizeImageParams.DEFAULT);
	}

	public ResizeImagePipe(ResizeImageParams params) {
		setParams(params);
	}

	/**
	* Process this pipe
	*
	* @param in {@link Mat} to be resized
	* @return Resized {@link Mat}
	*/
	@Override
	protected Mat process(Mat in) {
		Imgproc.resize(in, in, params.getSize());
		return in;
	}

	public static class ResizeImageParams {
		public static ResizeImageParams DEFAULT = new ResizeImageParams(320, 240);

		private Size size;
		public int width;
		public int height;

		public ResizeImageParams() {
			this(DEFAULT.width, DEFAULT.height);
		}

		public ResizeImageParams(int width, int height) {
			this.width = width;
			this.height = height;
			size = new Size(new double[] {width, height});
		}

		public Size getSize() {
			return size;
		}
	}
}
