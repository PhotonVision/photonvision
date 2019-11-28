package com.chameleonvision.config;

import com.chameleonvision.vision.camera.USBCameraCapture;
import com.chameleonvision.vision.camera.USBCameraProperties;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CameraJsonConfig {
    public final double fov;
    public final String path;
    public final String name;
    public final String nickname;

    @JsonCreator
    public CameraJsonConfig(
            @JsonProperty("fov") double fov,
            @JsonProperty("path") String path,
            @JsonProperty("name") String name,
            @JsonProperty("nickname") String nickname) {
        this.fov = fov;
        this.path = path;
        this.name = name;
        this.nickname = nickname;
    }

    public CameraJsonConfig(String path, String name) {
        this.fov = USBCameraProperties.DEFAULT_FOV;
        this.path = path;
        this.name = name;
        this.nickname = name;
    }

    public static CameraJsonConfig fromUSBCameraProcess(USBCameraCapture process) {
        USBCameraProperties camProps = process.getProperties();
        return new CameraJsonConfig(camProps.getFOV(), camProps.path, camProps.name, camProps.getNickname());
    }
}
