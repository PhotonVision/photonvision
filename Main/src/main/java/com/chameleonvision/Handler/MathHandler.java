package com.chameleonvision.Handler;
import java.lang.Math;
public class MathHandler {
    MathHandler(){}
    public static double sigmoid(double x){
        double bias = 0;
        double a = 5;
        double b = -0.05;
        double k = 200;
        if (x < 50){
            bias = -1.339;
        }
        return ((k / (1 + Math.pow(Math.E,(a + (b * x))))) + bias);
    }
}
