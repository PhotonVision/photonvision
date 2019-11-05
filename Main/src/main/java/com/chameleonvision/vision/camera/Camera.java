package com.chameleonvision.vision.camera;

import com.chameleonvision.settings.Platform;
import com.chameleonvision.vision.Pipeline;
import com.chameleonvision.web.ServerHandler;
import edu.wpi.cscore.*;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import org.opencv.core.Mat;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Camera {

    private static final double DEFAULT_FOV = 60.8;
    private static final StreamDivisor DEFAULT_STREAMDIVISOR = StreamDivisor.none;
    public static final int DEFAULT_EXPOSURE = 50;
    public static final int DEFAULT_BRIGHTNESS = 50;
    private static final int MINIMUM_FPS = 30;
    private static final int MINIMUM_WIDTH = 320;
    private static final int MINIMUM_HEIGHT = 200;
    private static final int MAX_INIT_MS = 1500;
    private static final List<VideoMode.PixelFormat> ALLOWED_PIXEL_FORMATS = Arrays.asList(VideoMode.PixelFormat.kYUYV, VideoMode.PixelFormat.kMJPEG);

    public final String name;
    public final String path;

    private String nickname;

    private final UsbCamera UsbCam;
    private final VideoMode[] availableVideoModes;

    private final CameraServer cs = CameraServer.getInstance();
    private final CvSink cvSink;
    private final Object cvSourceLock = new Object();
    private CvSource cvSource;
    private Double FOV;
    private StreamDivisor streamDivisor;
    private CameraValues camVals;
    private CamVideoMode camVideoMode;
    private int currentPipelineIndex;
    private List<Pipeline> pipelines;

    //Driver mode camera settings
    private int driverExposure;
    private int driverBrightness;
    private boolean isDriver;

    public Camera(String cameraName) {
        this(cameraName, DEFAULT_FOV);
    }

    public Camera(String cameraName, double fov) {
        this(cameraName, CameraManager.AllUsbCameraInfosByName.get(cameraName), fov);
    }

    public Camera(String cameraName, UsbCameraInfo usbCameraInfo, double fov) {
        this(cameraName, usbCameraInfo, fov, DEFAULT_STREAMDIVISOR);
    }

    public Camera(String cameraName, UsbCameraInfo usbCamInfo, double fov, StreamDivisor divisor) {
        this(cameraName, usbCamInfo, fov, new ArrayList<>(), 0, divisor, false);
    }

    public Camera(String cameraName, double fov, List<Pipeline> pipelines, int videoModeIndex, StreamDivisor divisor, boolean isDriver) {
        this(cameraName, CameraManager.AllUsbCameraInfosByName.get(cameraName), fov, pipelines, videoModeIndex, divisor, isDriver);
    }

    public Camera(String cameraName, double fov, int videoModeIndex, StreamDivisor divisor, boolean isDriver) {
        this(cameraName, fov, new ArrayList<>(), videoModeIndex, divisor, isDriver);
    }

    public Camera(String cameraName, UsbCameraInfo usbCamInfo, double fov, List<Pipeline> pipelines, int videoModeIndex, StreamDivisor divisor, boolean isDriver) {
        FOV = fov;
        name = cameraName;

        if (Platform.getCurrentPlatform().isWindows()) {
            path = usbCamInfo.path;
        } else {
            var truePath = Arrays.stream(usbCamInfo.otherPaths).filter(x -> x.contains("/dev/v4l/by-path")).findFirst();
            path = truePath.orElse(null);
        }

        streamDivisor = divisor;
        UsbCam = new UsbCamera(name, path);

        this.pipelines = pipelines;

        // set up video modes according to minimums
        if (Platform.getCurrentPlatform() == Platform.WINDOWS_64 && !UsbCam.isConnected()) {
            System.out.print("Waiting on camera... ");
            long initTimeout = System.nanoTime();
            while (!UsbCam.isConnected()) {
                if (((System.nanoTime() - initTimeout) / 1e6) >= MAX_INIT_MS) {
                    break;
                }
            }
            var initTimeMs = (System.nanoTime() - initTimeout) / 1e6;
            System.out.printf("Camera initialized in %.2fms\n", initTimeMs);
        }
        var trueVideoModes = UsbCam.enumerateVideoModes();
        availableVideoModes = Arrays.stream(trueVideoModes).filter(v ->
                v.fps >= MINIMUM_FPS && v.width >= MINIMUM_WIDTH && v.height >= MINIMUM_HEIGHT && ALLOWED_PIXEL_FORMATS.contains(v.pixelFormat)).toArray(VideoMode[]::new);
        if (availableVideoModes.length == 0) {
            System.err.println("Camera not supported!");
            throw new RuntimeException(new CameraException(CameraException.CameraExceptionType.BAD_CAMERA));
        }
        if (videoModeIndex <= availableVideoModes.length - 1) {
            setCamVideoMode(videoModeIndex, false);
        } else {
            setCamVideoMode(0, false);
        }

        cvSink = cs.getVideo(UsbCam);
        cvSource = cs.putVideo(name, camVals.ImageWidth / streamDivisor.value , camVals.ImageHeight / streamDivisor.value);
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

        // update camera values
        camVals = new CameraValues(this);

        boolean hasPrevVideoMode = prevVideoMode != null;
        boolean newVideoModeIsNew = hasPrevVideoMode && !prevVideoMode.equals(newVideoMode);

        if (newVideoModeIsNew || !hasPrevVideoMode) {
            UsbCam.setVideoMode(newVideoMode.getActualPixelFormat(), newVideoMode.width, newVideoMode.height, newVideoMode.fps);
            if (updateCvSource) {
                updateCvSource();
            }
        }
    }

    private void updateCvSource() {
        CameraManager.getVisionProcessByCameraName(name).cameraProcess.updateFrameSize();
        synchronized (cvSourceLock) {
            var newWidth = camVideoMode.width / streamDivisor.value;
            var newHeight = camVideoMode.height / streamDivisor.value;
            cvSource = cs.putVideo(name, newWidth, newHeight);
        }
        ServerHandler.sendFullSettings();
    }

    public void addPipeline() {
        Pipeline p = new Pipeline();
        p.nickname = "New pipeline " + pipelines.size();
        addPipeline(p);
    }

    public void addPipeline(Pipeline p) {
        this.pipelines.add(p);
    }

    public void deletePipeline(int index) {
        pipelines.remove(index);
    }

    public void deletePipeline() {
        deletePipeline(getCurrentPipelineIndex());
    }

    public Pipeline getCurrentPipeline() {
        return getPipelineByIndex(currentPipelineIndex);
    }

    public Pipeline getPipelineByIndex(int pipelineIndex) {
        return pipelines.get(pipelineIndex);
    }

    public int getCurrentPipelineIndex() {
        return currentPipelineIndex;
    }

    public void setCurrentPipelineIndex(int pipelineNumber) {
        if (pipelineNumber - 1 > pipelines.size()) return;
        currentPipelineIndex = pipelineNumber;
    }

    public StreamDivisor getStreamDivisor() {
        return streamDivisor;
    }

    public void setStreamDivisor(int divisor, boolean updateCvSource) {
        streamDivisor = StreamDivisor.values()[divisor];
        if (updateCvSource) {
            updateCvSource();
        }
    }

    public List<Pipeline> getPipelines() {
        return pipelines;
    }

    public List<String> getPipelinesNickname() {
        var pipelines = getPipelines();
        return pipelines.stream().map(pipeline -> pipeline.nickname).collect(Collectors.toList());
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

    public void setFOV(Number fov) {
        FOV = fov.doubleValue();
        camVals = new CameraValues(this);
    }

    public void setDriverMode(boolean state)
    {
        isDriver = state;
        if( isDriver ) {
            setBrightness(driverBrightness); //We call setBrightness because it updates after 2 calls
            setBrightness(driverBrightness); //Check it after we update to 2020 libraries
            setExposure(driverExposure);
        }
        else{
            UsbCam.setBrightness(getCurrentPipeline().brightness);
            UsbCam.setBrightness(getCurrentPipeline().brightness);
            try{UsbCam.setExposureManual(getCurrentPipeline().exposure);}
            catch (VideoException e)
            {
                System.out.println("Exposure change isn't supported");
            }
        }
    }

    public boolean getDriverMode()
    {
        return isDriver;
    }

    public int getBrightness() {
        return UsbCam.getBrightness();
    }



    public void setBrightness(int brightness) {
        if (isDriver) {
            driverBrightness = brightness;
            UsbCam.setBrightness(brightness); // set twice to reduce timeout
        }
        else {
            getCurrentPipeline().brightness = brightness;
        }
        UsbCam.setBrightness(brightness);
    }

    public void setExposure(int exposure) {
        if (isDriver) {
            driverExposure = exposure;
        }
        else {
            getCurrentPipeline().exposure = exposure;
        }

        try {
            UsbCam.setExposureManual(exposure);
        } catch (VideoException e) {
            System.err.println("Camera Does not support exposure change");
        }
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

    public List<HashMap> getResolutionList() {
        return Arrays.stream(availableVideoModes)
                .map(res -> new HashMap<String,Object>(){{
                    put("width", res.width);
                    put("height", res.height);
                    put("fps", res.fps);
                    put("pixelFormat", res.pixelFormat);
                }})
                .collect(Collectors.toList());
    }

    public void setNickname(String newNickname) {
        //Deletes old camera nt table
        NetworkTableInstance.getDefault().getTable("/chameleon-vision/" + this.nickname).getInstance().deleteAllEntries();
        nickname = newNickname;
        if (CameraManager.AllVisionProcessesByName.containsKey(this.name)) {
            NetworkTable newNT = NetworkTableInstance.getDefault().getTable("/chameleon-vision/" + this.nickname);
            CameraManager.AllVisionProcessesByName.get(this.name).resetNT(newNT);
        }
    }

    public String getNickname() {
        return nickname == null ? name : nickname;
    }

    public void setDriverExposure(int exposure) {
        driverExposure = exposure;

        if (isDriver) {
            setExposure(exposure);
        }
    }

    public void setDriverBrightness(int brightness) {
        driverBrightness = brightness;

        if (isDriver) {
            setBrightness(brightness);
        }
    }

    public int getDriverExposure() {
        return driverExposure;
    }

    public int getDriverBrightness() {
        return driverBrightness;
    }
}
