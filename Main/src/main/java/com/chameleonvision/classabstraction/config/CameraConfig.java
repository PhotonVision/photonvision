package com.chameleonvision.classabstraction.config;

public class CameraConfig {
    public double fov;
    public String path;
    public String name;
    public String nickname;

    public CameraConfig(double FOV,
                        String path, String name,
                        String nickname) {
        this.fov = FOV;
        this.path = path;
        this.name = name;
        this.nickname = nickname;
    }

}
