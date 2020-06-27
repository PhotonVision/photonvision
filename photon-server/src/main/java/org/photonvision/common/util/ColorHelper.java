package org.photonvision.common.util;

import java.awt.*;
import org.opencv.core.Scalar;

public class ColorHelper {
    public static Scalar colorToScalar(Color color) {
        return new Scalar(color.getBlue(), color.getGreen(), color.getRed());
    }
}
