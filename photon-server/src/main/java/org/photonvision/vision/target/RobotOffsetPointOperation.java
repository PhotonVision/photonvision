package org.photonvision.vision.target;

public enum RobotOffsetPointOperation {
    ROPO_CLEAR(0),
    ROPO_TAKESINGLE(1),
    ROPO_TAKEFIRSTDUAL(2),
    ROPO_TAKESECONDDUAL(3);

    public final int index;

    RobotOffsetPointOperation(int index) {
        this.index = index;
    }

    public static RobotOffsetPointOperation fromIndex(int index) {
        switch (index) {
            case 0: return ROPO_CLEAR;
            case 1: return ROPO_TAKESINGLE;
            case 2: return ROPO_TAKEFIRSTDUAL;
            case 3: return ROPO_TAKESECONDDUAL;
            default: return ROPO_CLEAR;
        }
    }
}
