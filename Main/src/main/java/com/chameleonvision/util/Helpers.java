package com.chameleonvision.util;

import org.opencv.core.Scalar;

import java.awt.*;

public class Helpers {
    private Helpers() {}

    public static Scalar colorToScalar(Color color) {
        return new Scalar(color.getRed(), color.getGreen(), color.getBlue());
    }

}
