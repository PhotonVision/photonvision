package org.photonvision.vision.camera;

public class UniqueCameraSummary {
    public final String uniqueName;
    public final String name;
    public final String path;
    public final String[] otherPaths;
    public final String type;

    public UniqueCameraSummary(String uniqueName, String name, String path, String[] otherPaths, CameraType type) {
        this.uniqueName = uniqueName;
        this.name = name;
        this.path = path;
        this.otherPaths = otherPaths;
        this.type = type.name();
    }

    public UniqueCameraSummary(String uniqueName, PVCameraInfo info) {
        this(uniqueName, info.name(), info.path(), info.otherPaths(), info.type());
    }
}
