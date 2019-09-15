package com.chameleonvision.vision;

import java.util.HashMap;

public class Camera {
    public Double FOV = 60.8;
    public String path = "";
    public HashMap<String, Pipeline> pipelines;
    public int resolution = 0;
    public CamVideoMode camVideoMode;

}
