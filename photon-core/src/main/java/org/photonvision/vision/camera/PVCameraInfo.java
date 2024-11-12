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

import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.hardware.Platform;

public sealed interface PVCameraInfo {
    /**
     * @return The path of the camera. This is the path that is used to open the camera.
     */
    String path();

    /**
     * @return The base name of the camera aka the name as just ascii.
     */
    String name();

    /**
     * @return Returns a human readable name
     */
    default String humanReadableName() {
        return name().replaceAll(" ", "_");
    }

    /**
     * If the camera is a USB camera this method returns
     * a unique descriptor of the USB port this camera is attached to. EG
     * "/dev/v4l/by-path/platform-fc800000.usb-usb-0:1.3:1.0-video-index0".
     * If the camera is a CSI camera this method returns the path of the camera.
     *
     * @return The unique path of the camera
     */
    String uniquePath();

    String[] otherPaths();

    CameraType type();

    public static final class PVUsbCameraInfo extends UsbCameraInfo implements PVCameraInfo {

        private PVUsbCameraInfo(
                int dev, String path, String name, String[] otherPaths, int vendorId, int productId) {
            super(dev, path, name, otherPaths, vendorId, productId);
        }

        private PVUsbCameraInfo(UsbCameraInfo info) {
            super(info.dev, info.path, info.name, info.otherPaths, info.vendorId, info.productId);
        }

        @Override
        public String path() {
            return super.path;
        }

        @Override
        public String name() {
            return super.name.replaceAll("[^\\x00-\\x7F]", "");
        }

        @Override
        public String uniquePath() {
            return Arrays.stream(super.otherPaths).filter(path -> path.contains("/by-path/")).findFirst()
                    .orElse(path());
        }

        @Override
        public String[] otherPaths() {
            return super.otherPaths;
        }

        @Override
        public CameraType type() {
            return CameraType.UsbCamera;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            PVUsbCameraInfo other = (PVUsbCameraInfo) obj;

            // Windows device number is not significant. See
            // https://github.com/wpilibsuite/allwpilib/blob/4b94a64b06057c723d6fcafeb1a45f55a70d179a/cscore/src/main/native/windows/UsbCameraImpl.cpp#L1128
            if (!Platform.isWindows()) {
                if (dev != other.dev) return false;
            }

            if (!path.equals(other.path)) return false;
            if (!name.equals(other.name)) return false;
            if (!this.uniquePath().contains(other.uniquePath())) return false;
            if (vendorId != other.vendorId) return false;
            if (productId != other.productId) return false;

            // Don't trust super.equals, as it compares references. Should PR this to allwpilib at some point
            return true;
        }
    }

    public static final class PVCSICameraInfo implements PVCameraInfo {
        public final String path;
        public final String baseName;

        private PVCSICameraInfo(String path, String baseName) {
            this.path = path;
            this.baseName = baseName;
        }

        @Override
        public String path() {
            return path;
        }

        @Override
        public String name() {
            return baseName.replaceAll("[^\\x00-\\x7F]", "");
        }

        @Override
        public String uniquePath() {
            return path();
        }

        @Override
        public String[] otherPaths() {
            return new String[0];
        }

        @Override
        public CameraType type() {
            return CameraType.ZeroCopyPicam;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            PVCSICameraInfo other = (PVCSICameraInfo) obj;

            if (!path.equals(other.path)) return false;
            if (!baseName.equals(other.baseName)) return false;

            return true;
        }
    }

    public static final class PVReconstructedCameraInfo implements PVCameraInfo {
        private final String path;
        private final String name;
        private final String[] otherPaths;
        private final CameraType type;

        public PVReconstructedCameraInfo(String path, String name, String[] otherPaths, CameraType type) {
            this.path = path;
            this.name = name;
            this.otherPaths = otherPaths;
            this.type = type;
        }

        @Override
        public String path() {
            return path;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String uniquePath() {
            return path;
        }

        @Override
        public String[] otherPaths() {
            return otherPaths;
        }

        @Override
        public CameraType type() {
            return type;
        }
    }

    public static PVCameraInfo fromUsbCameraInfo(UsbCameraInfo info) {
        return new PVUsbCameraInfo(info);
    }

    public static PVCameraInfo fromUsbCameraInfo(
        int dev, String path, String name, String[] otherPaths, int vendorId, int productId) {
        return new PVUsbCameraInfo(dev, path, name, otherPaths, vendorId, productId);
    }

    public static PVCameraInfo fromCSICameraInfo(String path, String baseName) {
        return new PVCSICameraInfo(path, baseName);
    }

    public static PVCameraInfo fromCameraConfig(CameraConfiguration config) {
        return new PVReconstructedCameraInfo(config.path, config.baseName, config.otherPaths, config.cameraType);
    }
}
