package com.chameleonvision.vision.camera;

import com.chameleonvision.config.CameraJsonConfig;
import com.chameleonvision.util.Platform;
import com.chameleonvision.vision.image.CaptureProperties;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.VideoMode;
import edu.wpi.first.wpilibj.geometry.Rotation2d;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class USBCaptureProperties extends CaptureProperties {
    public static final double DEFAULT_FOV = 70;
    private static final int DEFAULT_EXPOSURE = 50;
    private static final int DEFAULT_BRIGHTNESS = 50;
    private static final int MINIMUM_FPS = 30;
    private static final int MINIMUM_WIDTH = 320;
    private static final int MINIMUM_HEIGHT = 200;
    private static final int MAX_INIT_MS = 1500;

    private static final int PS3EYE_VID = 1415;
    private static final int PS3EYE_PID = 2000;

    private static final List<VideoMode.PixelFormat> ALLOWED_PIXEL_FORMATS = Arrays.asList(VideoMode.PixelFormat.kYUYV, VideoMode.PixelFormat.kMJPEG);

    private static final Predicate<VideoMode> kMinFPSPredicate = (videoMode -> videoMode.fps >= MINIMUM_FPS);
    private static final Predicate<VideoMode> kMinSizePredicate = (videoMode -> videoMode.width >= MINIMUM_WIDTH && videoMode.height >= MINIMUM_HEIGHT);
    private static final Predicate<VideoMode> kPixelFormatPredicate = (videoMode -> ALLOWED_PIXEL_FORMATS.contains(videoMode.pixelFormat));

    public final String name;
    public final String path;
    public final List<VideoMode> videoModes;

    private final UsbCamera baseCamera;
    public final boolean isPS3Eye;

    private String nickname;
    private double FOV;

    USBCaptureProperties(UsbCamera baseCamera, CameraJsonConfig config) {
        FOV = config.fov;
        name = config.name;
        path = config.path;
        nickname = config.nickname;
        this.baseCamera = baseCamera;

        int usbVID = baseCamera.getInfo().vendorId;
        int usbPID = baseCamera.getInfo().productId;

        // wait for camera USB init on Windows, Windows USB is slow...
        if (Platform.CurrentPlatform == Platform.WINDOWS_64 && !baseCamera.isConnected()) {
            System.out.print("Waiting on camera... ");
            long initTimeout = System.nanoTime();
            while (!baseCamera.isConnected()) {
                if (((System.nanoTime() - initTimeout) / 1e6) >= MAX_INIT_MS) {
                    break;
                }
            }
            var initTimeMs = (System.nanoTime() - initTimeout) / 1e6;
            System.out.printf("USBCameraProcess initialized in %.2fms\n", initTimeMs);
        }

        isPS3Eye = (usbVID == PS3EYE_VID && usbPID == PS3EYE_PID);
        videoModes = filterVideoModes(baseCamera.enumerateVideoModes());
    }

    public void setFOV(double FOV) {
        if (this.FOV != FOV) {
            this.FOV = FOV;
            staticProperties = new CaptureStaticProperties(staticProperties.mode, FOV);
        }
    }

    public double getFOV() {
        return FOV;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }

    private List<VideoMode> filterVideoModes(VideoMode[] videoModes) {
        Predicate<VideoMode> fullPredicate = kMinFPSPredicate.and(kMinSizePredicate).and(kPixelFormatPredicate);
        Stream<VideoMode> validModes = Arrays.stream(videoModes).filter(fullPredicate);
        return validModes.collect(Collectors.toList());
    }

    void updateVideoMode(VideoMode videoMode) {
        staticProperties = new CaptureStaticProperties(videoMode, FOV);
    }

    public List<VideoMode> getVideoModes() {
        return videoModes;
    }

    public VideoMode getVideoMode(int index){
        return videoModes.get(index);
    }

    public VideoMode getCurrentVideoMode() { return staticProperties.mode; }

    public int getCurrentVideoModeIndex(){
        return getVideoModes().indexOf(getCurrentVideoMode());
    }



}
