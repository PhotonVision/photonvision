package com.chameleonvision.vision.process;

import com.chameleonvision.vision.camera.Camera;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class CameraProcess implements Runnable {

    private final Camera camera;
    private final int maxFPS;
    private Mat inputFrame;
    private Mat outputFrame;
    private long timestamp;

    private final Object inputFrameLock = new Object();
    private final Object outputFrameLock = new Object();

    public CameraProcess(Camera camera) {
        this.camera = camera;
        maxFPS = camera.getVideoMode().fps;
        var camVals = camera.getCamVals();
        inputFrame = new Mat(camVals.ImageWidth, camVals.ImageHeight, CvType.CV_8UC3);
        outputFrame = new Mat(camVals.ImageWidth, camVals.ImageHeight, CvType.CV_8UC3);
    }

    void updateFrame(Mat inputFrame) {
        synchronized (inputFrameLock) {
            inputFrame.copyTo(this.inputFrame);
        }
    }

    long getLatestFrame(Mat outputFrame) {
        synchronized (outputFrameLock) {
            this.outputFrame.copyTo(outputFrame);
            return timestamp;
        }
    }

    @Override
    public void run() {
        while(!Thread.interrupted()) {
            synchronized (outputFrameLock) {
                timestamp = camera.grabFrame(outputFrame);
            }
            synchronized (inputFrameLock) {
                camera.putFrame(inputFrame);
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
