package com.chameleonvision.vision.camera;

import com.chameleonvision.vision.Pipeline;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.UsbCameraInfo;
import edu.wpi.cscore.VideoMode;

import java.util.HashMap;
import java.util.stream.IntStream;

public class Camera {

    private static double defaultFOV = 60.8;

    public final String name;
    public final String path;

    public final UsbCamera UsbCam;
    private final UsbCameraInfo UsbCamInfo;
    private final VideoMode[] availableVideoModes;

    private double FOV;

    private CameraValues camVals;
    private CamVideoMode camVideoMode;

    private int currentPipelineIndex;
    private HashMap<Integer, Pipeline> pipelines = new HashMap<>();

    public Camera(String cameraName) {
        this(cameraName, defaultFOV);
    }

    public Camera(UsbCameraInfo usbCamInfo) {
        this(usbCamInfo, defaultFOV);
    }

    public Camera(String cameraName, double fov) {
        this(CameraManager.AllUsbCameraInfosByName.get(cameraName), fov);
    }

    public Camera(UsbCameraInfo usbCamInfo, double fov) {
        UsbCamInfo = usbCamInfo;
        FOV = fov;
        name = usbCamInfo.name;
        path = usbCamInfo.path;

        UsbCam = new UsbCamera(name, path);

        // set up video mode
        availableVideoModes = UsbCam.enumerateVideoModes();
        setCamVideoMode(new CamVideoMode(availableVideoModes[0]));
    }

    public void setCamVideoMode(int videoMode) {
        setCamVideoMode(UsbCam.enumerateVideoModes()[videoMode]);
    }

    private void setCamVideoMode(VideoMode videoMode) {
        setCamVideoMode(new CamVideoMode(videoMode));
    }

    private void setCamVideoMode(CamVideoMode camVideoMode) {
        this.camVideoMode = camVideoMode;
        UsbCam.setPixelFormat(camVideoMode.getActualPixelFormat());
        UsbCam.setFPS(camVideoMode.fps);
        UsbCam.setResolution(camVideoMode.width, camVideoMode.height);
        camVals = new CameraValues(this);
        // TODO: Automatically restart CameraProcess when resolution changes (not FPS)
    }

    public void addPipeline() {
        addPipeline(pipelines.size());
    }

    private void addPipeline(int pipelineNumber) {
        if (pipelines.containsKey(pipelineNumber)) return;
        pipelines.put(pipelineNumber, new Pipeline());
    }

    public void setCurrentPipelineIndex(int pipelineNumber) {
        if (pipelineNumber - 1 > pipelines.size()) return;
        currentPipelineIndex = pipelineNumber;
    }

    public Pipeline getCurrentPipeline() {
        return pipelines.get(currentPipelineIndex);
    }

    public int getCurrentPipelineIndex() {
        return currentPipelineIndex;
    }

    public HashMap<Integer, Pipeline> getPipelines() {
        return pipelines;
    }

    public CamVideoMode getVideoMode() {
        return camVideoMode;
    }

    public int getVideoModeIndex() {
        return IntStream.range(0, availableVideoModes.length)
                .filter(i -> camVideoMode.isEqualToVideoMode(availableVideoModes[i]))
                .findFirst()
                .orElse(-1);
    }

    public void setFOV(double fov) {
        FOV = fov;
        camVals = new CameraValues(this);
    }

    public double getFOV() {
        return FOV;
    }

    public void setBrightness(int brightness) {
        getCurrentPipeline().brightness = brightness;
        UsbCam.setBrightness(brightness);
    }

    public int getBrightness() {
        return getCurrentPipeline().brightness;
    }

    public void setExposure(int exposure) {
        getCurrentPipeline().exposure = exposure;
        UsbCam.setExposureManual(exposure);
    }

}
