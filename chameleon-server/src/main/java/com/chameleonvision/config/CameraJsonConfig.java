package com.chameleonvision.config;

import com.chameleonvision.vision.VisionProcess;
import com.chameleonvision.vision.camera.USBCaptureProperties;
import com.chameleonvision.vision.enums.StreamDivisor;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CameraJsonConfig {
    public final double fov;
    public final String path;
    public final String name;
    public final String nickname;
    public final int videomode;
    public final StreamDivisor streamDivisor;

    @JsonCreator
    public CameraJsonConfig(
            @JsonProperty("fov") double fov,
            @JsonProperty("path") String path,
            @JsonProperty("name") String name,
            @JsonProperty("nickname") String nickname,
            @JsonProperty("videomode") int videomode,
            @JsonProperty("streamDivisor") StreamDivisor streamDivisor) {
        this.fov = fov;
        this.path = path;
        this.name = name;
        this.nickname = nickname;
        this.videomode = videomode;
        this.streamDivisor = streamDivisor;
    }

    public CameraJsonConfig(String path, String name) {
        this.fov = USBCaptureProperties.DEFAULT_FOV;
        this.path = path;
        this.name = name;
        this.nickname = name;
        this.videomode = 0;
        this.streamDivisor = StreamDivisor.NONE;
    }

    public static CameraJsonConfig fromVisionProcess(VisionProcess process) {
        USBCaptureProperties camProps = process.getCamera().getProperties();
        int videomode = camProps.getCurrentVideoModeIndex();
        StreamDivisor streamDivisor = process.cameraStreamer.getDivisor();
        return new CameraJsonConfig(camProps.getFOV(), camProps.path, camProps.name, camProps.getNickname(), videomode, streamDivisor);
    }
}
