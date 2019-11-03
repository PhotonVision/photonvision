package com.chameleonvision.vision.process;

import com.chameleonvision.vision.camera.Camera;
import com.chameleonvision.vision.camera.StreamDivisor;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class CameraProcess implements Runnable {

    private final Camera camera;

    private final Object outputFrameLock = new Object();
    private final Object inputFrameLock = new Object();
    private int maxFPS;
    private Mat outputFrame;
    private Mat inputFrame;
    private long timestamp;
    private StreamDivisor divisor;

    CameraProcess(Camera camera) {
        this.camera = camera;
        updateFrameSize();
    }

    public void updateFrameSize() {
        maxFPS = camera.getVideoMode().fps;
        divisor = camera.getStreamDivisor();
        var camVidMode = camera.getVideoMode();
        var newWidth = camVidMode.width / divisor.value;
        var newHeight = camVidMode.height / divisor.value;
        synchronized (outputFrameLock) {
            outputFrame = new Mat(newWidth, newHeight, CvType.CV_8UC3);
        }
        inputFrame = new Mat(camVidMode.width, camVidMode.height, CvType.CV_8UC3);
    }

    void setOutputFrame(Mat inputFrame) {
        synchronized (outputFrameLock) {
            inputFrame.copyTo(this.outputFrame);
        }
    }

    long getInputFrame(Mat inputFrame) {
        synchronized (inputFrameLock) {
            this.inputFrame.copyTo(inputFrame);
            return timestamp;
        }
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            synchronized (inputFrameLock) {
                timestamp = camera.grabFrame(inputFrame);
            }
            synchronized (outputFrameLock) {
                if (divisor.value != 1) {
                    var camVidMode = camera.getVideoMode();
                    var newWidth = camVidMode.width / divisor.value;
                    var newHeight = camVidMode.height / divisor.value;
                    Size newSize = new Size(newWidth, newHeight);
                    Imgproc.resize(outputFrame, outputFrame, newSize);
                }
                camera.putFrame(outputFrame);
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
