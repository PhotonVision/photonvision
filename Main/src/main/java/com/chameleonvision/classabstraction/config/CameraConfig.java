package com.chameleonvision.classabstraction.config;

public class CameraConfig {
    public final double fov;
    public final String path;
    public final String name;
    public final String nickname;

    public CameraConfig(double FOV,
                        String path, String name,
                        String nickname) {
        this.fov = FOV;
        this.path = path;
        this.name = name;
        this.nickname = nickname;
    }
}
