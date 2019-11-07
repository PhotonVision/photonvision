package com.chameleonvision.vision.process;

import com.chameleonvision.vision.Pipeline;
import com.chameleonvision.vision.camera.CamVideoMode;
import com.chameleonvision.vision.camera.CameraValues;
import com.chameleonvision.vision.camera.USBCamera;
import com.chameleonvision.vision.camera.StreamDivisor;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.List;

public class USBCameraProcess implements CameraProcess {

    private final USBCamera usbCamera;
    private final int maxFPS;
    private final Object inputFrameLock = new Object();
    private final Object outputFrameLock = new Object();
    private Mat inputFrame;
    private Mat outputFrame;
    private long timestamp;
    private StreamDivisor divisor;

    public USBCameraProcess(USBCamera usbCamera) {
        this.usbCamera = usbCamera;
        maxFPS = usbCamera.getVideoMode().fps;
        updateFrameSize();
    }

    public void updateFrameSize() {
        var camVals = usbCamera.getCamVals();
        divisor = usbCamera.getStreamDivisor();
        var newWidth = camVals.ImageWidth / divisor.value;
        var newHeight = camVals.ImageHeight / divisor.value;
        synchronized (inputFrameLock) {
            inputFrame = new Mat(newWidth, newHeight, CvType.CV_8UC3);
        }
        synchronized (outputFrameLock) {
            outputFrame = new Mat(newWidth, newHeight, CvType.CV_8UC3);
        }
    }

    public void updateFrame(Mat inputFrame) {
        synchronized (inputFrameLock) {
            inputFrame.copyTo(this.inputFrame);
        }
    }

    public long getLatestFrame(Mat outputFrame) {
        synchronized (outputFrameLock) {
            this.outputFrame.copyTo(outputFrame);
            return timestamp;
        }
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            synchronized (outputFrameLock) {
                timestamp = usbCamera.grabFrame(outputFrame);
            }
            synchronized (inputFrameLock) {
                if (divisor.value != 1) {
                    var camVals = usbCamera.getCamVals();
                    var newWidth = camVals.ImageWidth / divisor.value;
                    var newHeight = camVals.ImageHeight / divisor.value;
                    Size newSize = new Size(newWidth, newHeight);
                    Imgproc.resize(inputFrame, inputFrame, newSize);
                }
                usbCamera.putFrame(inputFrame);
            }
            var msToWait = (long) 1000 / maxFPS;
            try {
                Thread.sleep(msToWait);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // USBCameraProcess stuff

    @Override
    public String getCamName() {
        return usbCamera.name;
    }

    @Override
    public CameraValues getCamVals() {
        return usbCamera.getCamVals();
    }

    @Override
    public boolean getDriverMode() {
        return usbCamera.getDriverMode();
    }

    @Override
    public void setDriverMode(boolean isDriverMode) {
        usbCamera.setDriverMode(isDriverMode);
    }

    @Override
    public List<Pipeline> getPipelines() {
        return usbCamera.getPipelines();
    }

    @Override
    public Pipeline getCurrentPipeline() {
        return usbCamera.getCurrentPipeline();
    }

    @Override
    public int getCurrentPipelineIndex() {
        return usbCamera.getCurrentPipelineIndex();
    }

    @Override
    public void setExposure(int exposure) {
        usbCamera.setExposure(exposure);
    }

    @Override
    public void setBrightness(int brightness) {
        usbCamera.setBrightness(brightness);
    }

    @Override
    public CamVideoMode getVideoMode() {
        return usbCamera.getVideoMode();
    }

    @Override
    public String getNickname() {
        return usbCamera.getNickname();
    }

    @Override
    public void setCurrentPipelineIndex(int wantedIndex) {
        usbCamera.setCurrentPipelineIndex(wantedIndex);
    }
}
