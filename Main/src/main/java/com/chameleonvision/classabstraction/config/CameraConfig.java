package com.chameleonvision.classabstraction.config;

import com.chameleonvision.classabstraction.camera.CameraProperties;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CameraConfig {
    public final double fov;
    public final String path;
    public final String name;
    public final String nickname;

    @JsonCreator
    public CameraConfig(
            @JsonProperty("fov") double fov,
            @JsonProperty("path") String path,
            @JsonProperty("name") String name,
            @JsonProperty("nickname") String nickname) {
        this.fov = fov;
        this.path = path;
        this.name = name;
        this.nickname = nickname;
    }

    public CameraConfig(String path, String name) {
        this.fov = CameraProperties.DEFAULT_FOV;
        this.path = path;
        this.name = name;
        this.nickname = name;
    }
}
