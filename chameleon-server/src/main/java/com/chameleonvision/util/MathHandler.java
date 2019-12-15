package com.chameleonvision.util;

import java.lang.Math;

import edu.wpi.first.wpiutil.math.Num;
import org.apache.commons.math3.util.FastMath;

public class MathHandler {
    MathHandler() {}

    public static double sigmoid(Number x){
        double bias = 0;
        double a = 5;
        double b = -0.05;
        double k = 200;

        if (x.doubleValue() < 50){
            bias = -1.338;
        }

        return ((k / (1 + Math.pow(Math.E,(a + (b * x.doubleValue()))))) + bias);
    }

    public static double toSlope(Number angle){
        return FastMath.atan(FastMath.toRadians(angle.doubleValue() - 90));
    }

    public static double roundTo(double value, int to) {
        double toMult = Math.pow(10, to);
        return (double)Math.round(value * toMult) / toMult;
    }
}
