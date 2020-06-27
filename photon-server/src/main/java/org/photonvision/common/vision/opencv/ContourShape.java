package org.photonvision.common.vision.opencv;

import java.util.EnumSet;
import java.util.HashMap;

public enum ContourShape {
    Custom(-1),
    Circle(0),
    Triangle(3),
    Quadrilateral(4);

    public final int sides;

    ContourShape(int sides) {
        this.sides = sides;
    }

    private static final HashMap<Integer, ContourShape> sidesToValueMap = new HashMap<>();

    static {
        for (var value : EnumSet.allOf(ContourShape.class)) {
            sidesToValueMap.put(value.sides, value);
        }
    }

    public static ContourShape fromSides(int sides) {
        return sidesToValueMap.get(sides);
    }
}
