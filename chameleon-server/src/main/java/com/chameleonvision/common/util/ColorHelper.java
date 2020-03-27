package com.chameleonvision.common.util;

import org.opencv.core.Scalar;

import java.awt.*;

public class ColorHelper {
    public static Scalar colorToScalar(Color color) {
        return new Scalar(color.getBlue(), color.getGreen(), color.getRed());
    }
}
