package com.chameleonvision.Handler;
import org.apache.commons.math3.util.FastMath;

import java.lang.Math;
public class MathHandler {
    MathHandler(){}
    public static double sigmoid(double x){
        double bias = 0;
        double a = 5;
        double b = -0.05;
        double k = 200;
        if (x < 50){
            bias = -1.338;
        }
        return ((k / (1 + Math.pow(Math.E,(a + (b * x))))) + bias);
    }
    public static double toSlope(double angle){
        return FastMath.atan(FastMath.toRadians(angle - 90));
    }
}
