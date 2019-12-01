package com.chameleonvision.util;

import edu.wpi.cscore.VideoMode;
import org.opencv.core.Scalar;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class Helpers {
    private Helpers() {
    }

    public static Scalar colorToScalar(Color color) {
        return new Scalar(color.getRed(), color.getGreen(), color.getBlue());
    }

    public static HashMap VideoModeToHashMap(VideoMode videoMode) {
        return new HashMap<String, Object>() {{
                put("width", videoMode.width);
                put("height", videoMode.height);
                put("fps", videoMode.fps);
                put("pixelFormat", videoMode.pixelFormat.toString());}};
    }
}
