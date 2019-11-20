package com.chameleonvision.classabstraction.camera;

import com.chameleonvision.Main;
import com.chameleonvision.classabstraction.config.CameraConfig;
import com.chameleonvision.settings.Platform;
import com.chameleonvision.vision.camera.USBCamera;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.UsbCameraInfo;
import edu.wpi.cscore.VideoMode;
import org.apache.commons.math3.util.FastMath;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CameraProperties {
    private static final double DEFAULT_FOV = 70;
    private static final int DEFAULT_EXPOSURE = 50;
    private static final int DEFAULT_BRIGHTNESS = 50;
    private static final int MINIMUM_FPS = 30;
    private static final int MINIMUM_WIDTH = 320;
    private static final int MINIMUM_HEIGHT = 200;
    private static final int MAX_INIT_MS = 1500;
    private static final List<VideoMode.PixelFormat> ALLOWED_PIXEL_FORMATS = Arrays.asList(VideoMode.PixelFormat.kYUYV, VideoMode.PixelFormat.kMJPEG);

    private static final Predicate<VideoMode> kMinFPSPredicate = (videoMode -> videoMode.fps >= MINIMUM_FPS);
    private static final Predicate<VideoMode> kMinSizePredicate = (videoMode -> videoMode.width >= MINIMUM_WIDTH && videoMode.height >= MINIMUM_HEIGHT);
    private static final Predicate<VideoMode> kPixelFormatPredicate = (videoMode -> ALLOWED_PIXEL_FORMATS.contains(videoMode.pixelFormat));

    public CameraStaticProperties staticProperties;
    public final String name;
    public final String path;
    public final double FOV;
    public final List<VideoMode> videoModes;

    private final UsbCamera baseCamera;

    private String nickname;

    public final boolean hasGain;

    public CameraProperties(UsbCamera baseCamera, CameraConfig config) {
        FOV = config.fov;
        name = config.name;
        path = config.path;
        nickname = config.nickname;
        this.baseCamera = baseCamera;

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

        hasGain = false;
        var props = baseCamera.enumerateProperties();

        videoModes = filterVideoModes(baseCamera.enumerateVideoModes());
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

    public void updateVideoMode(VideoMode videoMode) {
        staticProperties = new CameraStaticProperties(videoMode.width, videoMode.height, FOV);
    }

    public double CalculatePitch(double PixelY, double centerY) {
        double pitch = FastMath.toDegrees(FastMath.atan((PixelY - centerY) / staticProperties.verticalFocalLength));
        return (pitch * -1);
    }

    public double CalculateYaw(double PixelX, double centerX) {
        return FastMath.toDegrees(FastMath.atan((PixelX - centerX) / staticProperties.horizontalFocalLength));
    }
}
