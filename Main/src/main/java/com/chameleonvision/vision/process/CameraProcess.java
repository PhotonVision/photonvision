package com.chameleonvision.vision.process;

import com.chameleonvision.vision.camera.Camera;
import com.chameleonvision.vision.camera.StreamDivisor;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class CameraProcess implements Runnable {

    private final Camera camera;
    private final int maxFPS;
    private final Object inputFrameLock = new Object();
    private final Object outputFrameLock = new Object();
    private Mat inputFrame;
    private Mat outputFrame;
    private long timestamp;
    private StreamDivisor divisor;

    CameraProcess(Camera camera) {
        this.camera = camera;
        maxFPS = camera.getVideoMode().fps;
        updateFrameSize();
    }

    public void updateFrameSize() {
        var camVals = camera.getCamVals();
        divisor = camera.getStreamDivisor();
        var newWidth = camVals.ImageWidth / divisor.value;
        var newHeight = camVals.ImageHeight / divisor.value;
        synchronized (inputFrameLock) {
            inputFrame = new Mat(newWidth, newHeight, CvType.CV_8UC3);
        }
        synchronized (outputFrameLock) {
            outputFrame = new Mat(newWidth, newHeight, CvType.CV_8UC3);
        }
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
        while (!Thread.interrupted()) {
            synchronized (outputFrameLock) {
                timestamp = camera.grabFrame(outputFrame);
            }
            synchronized (inputFrameLock) {
                if (divisor.value != 1) {
                    var camVals = camera.getCamVals();
                    var newWidth = camVals.ImageWidth / divisor.value;
                    var newHeight = camVals.ImageHeight / divisor.value;
                    Size newSize = new Size(newWidth, newHeight);
                    Imgproc.resize(inputFrame, inputFrame, newSize);
                }
                camera.putFrame(inputFrame);
            }
            var msToWait = (long) 1000 / maxFPS;
            try {
                Thread.sleep(msToWait);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
