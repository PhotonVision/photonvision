package com.chameleonvision.util;

import edu.wpi.cscore.VideoMode;
import org.opencv.core.Scalar;

import java.awt.*;

public class Helpers {
    private Helpers() {}

    public static Scalar colorToScalar(Color color) {
        return new Scalar(color.getRed(), color.getGreen(), color.getBlue());
    }

    public static String VideoModeToString(VideoMode videoMode) {
        return String.format("%dx%d@%dFPS in %s", videoMode.width, videoMode.height, videoMode.fps, videoMode.pixelFormat.toString());
    }
}
