package Objects;

import edu.wpi.cscore.UsbCameraInfo;

import java.util.HashMap;

public class Camera {
    public Double FOV = 60.8;
    public String path = "";
    public HashMap<String,DefaultPipeline> pipelines;
    public int resolution = 0;
    public VideoMode videoMode;

}
