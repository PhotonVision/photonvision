package com.chameleonvision.vision.process;

import com.chameleonvision.vision.camera.Camera;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class StreamProcess implements Runnable {

    private final Camera camera;
    private final int maxFPS;
    private Mat streamFrame;

    private final Object streamFrameLock = new Object();

    public StreamProcess(Camera camera) {
        this.camera = camera;
        maxFPS = camera.getVideoMode().fps;
        var camVals = camera.getCamVals();
        streamFrame = new Mat(camVals.ImageWidth, camVals.ImageHeight, CvType.CV_8UC3);
    }

    void updateFrame(Mat inputFrame) {
        synchronized (streamFrameLock) {
            inputFrame.copyTo(streamFrame);
        }
    }

    @Override
    public void run() {
        while(!Thread.interrupted()) {
            synchronized (streamFrameLock) {
                camera.putFrame(streamFrame);
            }
            var msToWait = (long)1000/maxFPS;
            try {
                Thread.sleep(msToWait);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
