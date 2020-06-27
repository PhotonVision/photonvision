package org.photonvision._2.config;

import org.photonvision._2.vision.VisionProcess;
import org.photonvision._2.vision.camera.USBCaptureProperties;
import org.photonvision._2.vision.enums.StreamDivisor;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CameraJsonConfig {
    public final double fov;
    public final String path;
    public final String name;
    public final String nickname;
    public final double tilt;
    public final int videomode;
    public final StreamDivisor streamDivisor;

    @JsonCreator
    public CameraJsonConfig(
            @JsonProperty("fov") double fov,
            @JsonProperty("path") String path,
            @JsonProperty("name") String name,
            @JsonProperty("nickname") String nickname,
            @JsonProperty("videomode") int videomode,
            @JsonProperty("streamDivisor") StreamDivisor streamDivisor,
            @JsonProperty("tilt") double tilt) {
        this.fov = fov;
        this.path = path;
        this.name = name;
        this.nickname = nickname;
        this.videomode = videomode;
        this.streamDivisor = streamDivisor;
        this.tilt = tilt;
    }

    public CameraJsonConfig(String path, String name) {
        this.fov = USBCaptureProperties.DEFAULT_FOV;
        this.path = path;
        this.name = name;
        this.nickname = name;
        this.videomode = 0;
        this.streamDivisor = StreamDivisor.NONE;
        this.tilt = 0;
    }

    public static CameraJsonConfig fromVisionProcess(VisionProcess process) {
        USBCaptureProperties camProps = process.getCamera().getProperties();
        int videomode = camProps.getCurrentVideoModeIndex();
        StreamDivisor streamDivisor = process.cameraStreamer.getDivisor();
        double tilt = process.getCamera().getProperties().getTilt().getDegrees();
        return new CameraJsonConfig(
                camProps.getFOV(),
                camProps.path,
                camProps.name,
                camProps.getNickname(),
                videomode,
                streamDivisor,
                tilt);
    }
}
