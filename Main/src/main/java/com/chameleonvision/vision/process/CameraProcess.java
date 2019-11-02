package com.chameleonvision.vision.process;

import com.chameleonvision.vision.Pipeline;
import com.chameleonvision.vision.camera.CamVideoMode;
import com.chameleonvision.vision.camera.CameraValues;
import org.opencv.core.Mat;

import java.util.List;

public interface CameraProcess extends Runnable {

    long getLatestFrame(Mat outputFrame);

    void updateFrame(Mat inputFrame);

    void updateFrameSize();

    String getCamName();
    CameraValues getCamVals();
    boolean getDriverMode();
    void setDriverMode(boolean isDriverMode);
    List<Pipeline> getPipelines();
    Pipeline getCurrentPipeline();
    int getCurrentPipelineIndex();
    void setExposure(int exposure);
    void setBrightness(int brightness);
    CamVideoMode getVideoMode();
    String getNickname();
    void setCurrentPipelineIndex(int ntPipelineIndex);

    PipelineResult runPipeline(Pipeline currentPipeline, Mat inputImage, Mat outputImage,
                               CameraValues cameraValues, boolean shouldFlip, boolean driverMode);

}
