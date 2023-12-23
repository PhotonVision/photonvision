package org.photonvision.vision.camera;

import java.util.Arrays;

import edu.wpi.first.cscore.UsbCameraInfo;

public class CameraInfo extends UsbCameraInfo {
    public final CameraType cameraType;
    public CameraInfo(int dev, String path, String name, String[] otherPaths, int vendorId, int productId) {
        super(dev, path, name, otherPaths, vendorId, productId);
        cameraType = CameraType.UsbCamera;
    }
    public CameraInfo(int dev, String path, String name, String[] otherPaths, int vendorId, int productId, CameraType cameraType) {
        super(dev, path, name, otherPaths, vendorId, productId);
        this.cameraType = cameraType;
    }

    /**
     * 
     * @return True, if this camera is reported from V4L and is a CSI camera.
     */
    public boolean getIsV4lCsiCamera() {
        if(otherPaths == null) return false;
        return (Arrays.stream(otherPaths).anyMatch(it -> it.contains("csi-video"))
                || getBaseName().equals("unicam"));
    }

    /**
     * 
     * @return The base name of the camera aka the name as just ascii.
     */
    public String getBaseName() {
        return name.replaceAll("[^\\x00-\\x7F]", "");
    }

    /**
     * 
     * @param baseName
     * @return Returns a human readable name
     */
    public String getHumanReadableName() {
        return getBaseName().replaceAll(" ", "_");
    }

    public boolean Equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof UsbCameraInfo))
            return false;
        UsbCameraInfo other = (UsbCameraInfo) o;
        return path.equals(other.path)
                // && a.dev == b.dev (dev is not constant in Windows)
                && name.equals(other.name)
                && productId == other.productId
                && vendorId == other.vendorId;
    }

}
