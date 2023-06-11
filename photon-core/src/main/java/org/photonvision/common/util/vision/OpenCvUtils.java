package org.photonvision.common.util.vision;

import edu.wpi.first.cscore.VideoMode;

public class OpenCvUtils {
    private OpenCvUtils() {}

    public static boolean videoModeEquals(VideoMode a, VideoMode b) {
        // WPILib doesn't provide an equals(), so implement our own here
        if (a.pixelFormat != b.pixelFormat) return false;
        if (a.width != b.width) return false;
        if (a.height != b.height) return false;
        if (a.fps != b.fps) return false;
        return true;
    }
}
