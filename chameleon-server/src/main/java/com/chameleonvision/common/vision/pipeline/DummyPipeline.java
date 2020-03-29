package com.chameleonvision.common.vision.pipeline;

import com.chameleonvision.common.vision.pipeline.pipe.ResizeImagePipe;
import com.chameleonvision.common.vision.pipeline.pipe.RotateImagePipe;
import edu.wpi.cscore.CameraServerCvJNI;
import java.io.IOException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

/** This class exists for the sole purpose of showing how pipes would interact in a pipeline */
public class DummyPipeline {
	private static ResizeImagePipe resizePipe = new ResizeImagePipe();
	private static RotateImagePipe rotatePipe = new RotateImagePipe();

	public static void main(String[] args) {
		try {
			CameraServerCvJNI.forceLoad();
		} catch (UnsatisfiedLinkError | IOException e) {
			throw new RuntimeException("Failed to load JNI Libraries!");
		}

		// obviously not a useful test, purely for example.
		Mat fakeCameraMat = new Mat(640, 480, CvType.CV_8UC3);

		PipeResult<Mat> resizeResult = resizePipe.apply(fakeCameraMat);
		PipeResult<Mat> rotateResult = rotatePipe.apply(resizeResult.result);

		long fullTime = resizeResult.nanosElapsed + rotateResult.nanosElapsed;
		System.out.println(fullTime / 1.0e+6 + "ms elapsed");
	}
}
