package com.chameleonvision.common.vision.opencv;

public enum ContourShape {
    Custom(-1),
    Circle(0),
    Triangle(3),
    Square(4),
    Rectangle(4);

    public final int sides;

    ContourShape(int sides) {
        this.sides = sides;
    }
}
