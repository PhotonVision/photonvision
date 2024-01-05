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

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof UsbCameraInfo || o instanceof CameraInfo)) return false;
        UsbCameraInfo other = (UsbCameraInfo) o;
        return path.equals(other.path)
                // && a.dev == b.dev (dev is not constant in Windows)
                && name.equals(other.name)
                && productId == other.productId
                && vendorId == other.vendorId;
    }
}
