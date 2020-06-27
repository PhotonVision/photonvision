package org.photonvision.common.vision.frame.provider;

import org.photonvision.common.vision.frame.Frame;
import org.photonvision.common.vision.frame.FrameProvider;
import org.photonvision.common.vision.frame.FrameStaticProperties;
import org.photonvision.common.vision.opencv.CVMat;
import edu.wpi.cscore.CvSink;

public class USBFrameProvider implements FrameProvider {
    private static int count = 0;
    private CvSink cvSink;
    private FrameStaticProperties frameStaticProperties;
    private CVMat mat;

    public USBFrameProvider(CvSink sink, FrameStaticProperties frameStaticProperties) {
        cvSink = sink;
        this.frameStaticProperties = frameStaticProperties;
        mat = new CVMat();
    }

    @Override
    public Frame get() {
        if (mat != null && mat.getMat() != null) {
            mat.release();
        }
        long time = cvSink.grabFrame(mat.getMat());
        return new Frame(mat, time, frameStaticProperties);
    }

    @Override
    public String getName() {
        return "USBFrameProvider" + count++;
    }
}
