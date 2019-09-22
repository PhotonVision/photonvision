package com.chameleonvision.vision.camera;

import com.chameleonvision.vision.Pipeline;
import com.chameleonvision.web.ServerHandler;
import edu.wpi.cscore.*;
import edu.wpi.first.cameraserver.CameraServer;
import org.opencv.core.Mat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.IntStream;

public class Camera {

	private static final double DEFAULT_FOV = 60.8;
	private static final int MINIMUM_FPS = 30;
	private static final int MINIMUM_WIDTH = 320;
	private static final int MINIMUM_HEIGHT = 240;

	public final String name;
	public final String path;

	private final UsbCamera UsbCam;
	private final VideoMode[] availableVideoModes;

	private final CameraServer cs = CameraServer.getInstance();
	private final CvSink cvSink;
	private final Object cvSourceLock = new Object();
	private CvSource cvSource;
	private double FOV;
	private CameraValues camVals;
	private CamVideoMode camVideoMode;
	private int currentPipelineIndex;
	private HashMap<Integer, Pipeline> pipelines;


	public Camera(String cameraName) {
		this(cameraName, DEFAULT_FOV);
	}

	public Camera(String cameraName, double fov) {
		this(CameraManager.AllUsbCameraInfosByName.get(cameraName), fov);
	}

	public Camera(UsbCameraInfo usbCamInfo, double fov) {
		this(usbCamInfo, fov, new HashMap<>(), 0);
	}

	public Camera(String cameraName, double fov, int videoModeIndex) {
		this(cameraName, fov, new HashMap<>(), videoModeIndex);
	}

	public Camera(String cameraName, double fov, HashMap<Integer, Pipeline> pipelines, int videoModeIndex) {
		this(CameraManager.AllUsbCameraInfosByName.get(cameraName), fov, pipelines, videoModeIndex);
	}

	public Camera(UsbCameraInfo usbCamInfo, double fov, HashMap<Integer, Pipeline> pipelines, int videoModeIndex) {
		FOV = fov;
		name = usbCamInfo.name;
		path = usbCamInfo.path;

		UsbCam = new UsbCamera(name, path);

		this.pipelines = pipelines;

		// set up video modes according to minimums
		availableVideoModes = Arrays.stream(UsbCam.enumerateVideoModes()).filter(v -> v.fps >= MINIMUM_FPS && v.width >= MINIMUM_WIDTH && v.height >= MINIMUM_HEIGHT).toArray(VideoMode[]::new);
		if (videoModeIndex <= availableVideoModes.length - 1) {
			setCamVideoMode(videoModeIndex, false);
		} else {
			setCamVideoMode(0, false);
		}

		cvSink = cs.getVideo(UsbCam);
		cvSource = cs.putVideo(name, camVals.ImageWidth, camVals.ImageHeight);
		var s = (MjpegServer) cs.getServer("serve_" + name);
		CameraManager.CameraPorts.put(name, s.getPort());
	}

	VideoMode[] getAvailableVideoModes() {
		return availableVideoModes;
	}

	public int getStreamPort() {
		var s = (MjpegServer) cs.getServer("serve_" + name);
		return s.getPort();
	}

	public void setCamVideoMode(int videoMode, boolean updateCvSource) {
		setCamVideoMode(new CamVideoMode(availableVideoModes[videoMode]), updateCvSource);
	}

	private void setCamVideoMode(CamVideoMode newVideoMode, boolean updateCvSource) {
		var prevVideoMode = this.camVideoMode;
		this.camVideoMode = newVideoMode;
		UsbCam.setPixelFormat(newVideoMode.getActualPixelFormat());
		UsbCam.setFPS(newVideoMode.fps);
		UsbCam.setResolution(newVideoMode.width, newVideoMode.height);

		// update camera values
		camVals = new CameraValues(this);
		if (prevVideoMode != null && !prevVideoMode.equals(newVideoMode) && updateCvSource) { //  if resolution changed
			synchronized (cvSourceLock) {
				cvSource = cs.putVideo(name, newVideoMode.width, newVideoMode.height);
			}
			ServerHandler.sendFullSettings();
		}
	}

	void addPipeline() {
		addPipeline(pipelines.size());
	}

	private void addPipeline(int pipelineNumber) {
		if (pipelines.containsKey(pipelineNumber)) return;
		pipelines.put(pipelineNumber, new Pipeline());
	}

	public Pipeline getCurrentPipeline() {
		return pipelines.get(currentPipelineIndex);
	}

	public int getCurrentPipelineIndex() {
		return currentPipelineIndex;
	}

	void setCurrentPipelineIndex(int pipelineNumber) {
		if (pipelineNumber - 1 > pipelines.size()) return;
		currentPipelineIndex = pipelineNumber;
	}

	public HashMap<Integer, Pipeline> getPipelines() {
		return pipelines;
	}

	public CamVideoMode getVideoMode() {
		return camVideoMode;
	}

	public int getVideoModeIndex() {
		return IntStream.range(0, availableVideoModes.length)
				.filter(i -> camVideoMode.equals(availableVideoModes[i]))
				.findFirst()
				.orElse(-1);
	}

	public double getFOV() {
		return FOV;
	}

	public void setFOV(double fov) {
		FOV = fov;
		camVals = new CameraValues(this);
	}

	public int getBrightness() {
		return getCurrentPipeline().brightness;
	}

	public void setBrightness(int brightness) {
		getCurrentPipeline().brightness = brightness;
		UsbCam.setBrightness(brightness);
	}

	public void setExposure(int exposure) {
		getCurrentPipeline().exposure = exposure;
		UsbCam.setExposureManual(exposure);
	}

	public long grabFrame(Mat image) {
		return cvSink.grabFrame(image);
	}

	public CameraValues getCamVals() {
		return camVals;
	}

	public void putFrame(Mat image) {
		synchronized (cvSourceLock) {
			cvSource.putFrame(image);
		}
	}
}
