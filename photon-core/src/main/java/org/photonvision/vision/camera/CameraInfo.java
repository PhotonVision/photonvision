/*
 * Copyright (C) Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.photonvision.vision.camera;

import edu.wpi.first.cscore.UsbCameraInfo;
import java.util.Arrays;
import java.util.Optional;
import org.photonvision.common.hardware.Platform;

public class CameraInfo extends UsbCameraInfo {
    public final CameraType cameraType;

    public CameraInfo(
            int dev, String path, String name, String[] otherPaths, int vendorId, int productId) {
        super(dev, path, name, otherPaths, vendorId, productId);
        cameraType = CameraType.UsbCamera;
    }

    public CameraInfo(
            int dev,
            String path,
            String name,
            String[] otherPaths,
            int vendorId,
            int productId,
            CameraType cameraType) {
        super(dev, path, name, otherPaths, vendorId, productId);
        this.cameraType = cameraType;
    }

    public CameraInfo(UsbCameraInfo info) {
        super(info.dev, info.path, info.name, info.otherPaths, info.vendorId, info.productId);
        cameraType = CameraType.UsbCamera;
    }

    /**
     * @return True, if this camera is reported from V4L and is a CSI camera.
     */
    public boolean getIsV4lCsiCamera() {
        return (Arrays.stream(otherPaths).anyMatch(it -> it.contains("csi-video"))
                || getBaseName().equals("unicam"));
    }

    /**
     * @return The base name of the camera aka the name as just ascii.
     */
    public String getBaseName() {
        return name.replaceAll("[^\\x00-\\x7F]", "");
    }

    /**
     * @return Returns a human readable name
     */
    public String getHumanReadableName() {
        return getBaseName().replaceAll(" ", "_");
    }

    /**
     * Get a unique descriptor of the USB port this camera is attached to. EG
     * "/dev/v4l/by-path/platform-fc800000.usb-usb-0:1.3:1.0-video-index0"
     *
     * @return
     */
    public Optional<String> getUSBPath() {
        return Arrays.stream(otherPaths).filter(path -> path.contains("/by-path/")).findFirst();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        CameraInfo other = (CameraInfo) obj;

        // Windows device number is not significant. See
        // https://github.com/wpilibsuite/allwpilib/blob/4b94a64b06057c723d6fcafeb1a45f55a70d179a/cscore/src/main/native/windows/UsbCameraImpl.cpp#L1128
        if (!Platform.isWindows()) {
            if (dev != other.dev) return false;
        }

        if (!path.equals(other.path)) return false;
        if (!name.equals(other.name)) return false;
        if (!Arrays.asList(this.otherPaths).containsAll(Arrays.asList(other.otherPaths))) return false;
        if (vendorId != other.vendorId) return false;
        if (productId != other.productId) return false;

        // Don't trust super.equals, as it compares references. Should PR this to allwpilib at some
        // point
        return true;
    }

    @Override
    public String toString() {
        return "CameraInfo [cameraType="
                + cameraType
                + ", baseName="
                + getBaseName()
                + ", vid="
                + vendorId
                + ", pid="
                + productId
                + ", path="
                + path
                + ", otherPaths="
                + Arrays.toString(otherPaths)
                + "]";
    }
}
