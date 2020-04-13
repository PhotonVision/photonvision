package com.chameleonvision.common.vision.frame;

import com.chameleonvision.common.vision.opencv.CVMat;
import com.chameleonvision.common.vision.opencv.Releasable;
import org.opencv.core.Mat;

public class Frame implements Releasable {
    public final long timestampNanos;
    public final CVMat image;
    public final FrameStaticProperties frameStaticProperties;

    public Frame(CVMat image, long timestampNanos, FrameStaticProperties frameStaticProperties) {
        this.image = image;
        this.timestampNanos = timestampNanos;
        this.frameStaticProperties = frameStaticProperties;
    }

    public Frame(CVMat image, FrameStaticProperties frameStaticProperties) {
        this(image, System.nanoTime(), frameStaticProperties);
    }

    public void copyTo(Mat destMat) {
        image.getMat().copyTo(destMat);
    }

    public static Frame copyFrom(Frame frame) {
        Mat newMat = new Mat();
        frame.image.getMat().copyTo(newMat);
        frame.release();
        return new Frame(new CVMat(newMat), frame.timestampNanos, frame.frameStaticProperties);
    }

    @Override
    public void release() {
        image.release();
    }
}
