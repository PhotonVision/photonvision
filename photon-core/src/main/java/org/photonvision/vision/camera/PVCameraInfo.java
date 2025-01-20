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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import edu.wpi.first.cscore.UsbCameraInfo;
import java.util.Arrays;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
    @JsonSubTypes.Type(value = PVCameraInfo.PVUsbCameraInfo.class),
    @JsonSubTypes.Type(value = PVCameraInfo.PVCSICameraInfo.class),
    @JsonSubTypes.Type(value = PVCameraInfo.PVFileCameraInfo.class)
})
public sealed interface PVCameraInfo {
    /**
     * @return The path of the camera.
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
     * If the camera is a USB camera this method returns a unique descriptor of the USB port this
     * camera is attached to. EG "/dev/v4l/by-path/platform-fc800000.usb-usb-0:1.3:1.0-video-index0".
     * If the camera is a CSI camera this method returns the path of the camera.
     *
     * <p>If we are on Windows, this will return the opaque path as described by
     * MF_DEVSOURCE_ATTRIBUTE_SOURCE_TYPE_VIDCAP_SYMBOLIC_LINK (see
     * https://learn.microsoft.com/en-us/windows/win32/medfound/mf-devsource-attribute-source-type-vidcap-symbolic-link)
     *
     * @return The unique path of the camera
     */
    @JsonGetter(value = "uniquePath")
    String uniquePath();

    String[] otherPaths();

    CameraType type();

    default boolean equals(PVCameraInfo other) {
        return uniquePath().equals(other.uniquePath());
    }

    @JsonTypeName("PVUsbCameraInfo")
    public static final class PVUsbCameraInfo extends UsbCameraInfo implements PVCameraInfo {
        @JsonCreator
        public PVUsbCameraInfo(
                @JsonProperty("dev") int dev,
                @JsonProperty("path") String path,
                @JsonProperty("name") String name,
                @JsonProperty("otherPaths") String[] otherPaths,
                @JsonProperty("vendorId") int vendorId,
                @JsonProperty("productId") int productId) {
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
            return Arrays.stream(super.otherPaths)
                    .sorted() // Must sort to ensure a consistent unique path as we can get more than one
                    // by-path and their order changes at random?
                    .filter(path -> path.contains("/by-path/"))
                    .findFirst()
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
            if (obj instanceof PVCameraInfo info) {
                return equals(info);
            }
            return false;
        }

        @Override
        public String toString() {
            return "PVUsbCameraInfo[type="
                    + type()
                    + ", dev="
                    + super.dev
                    + ", path='"
                    + super.path
                    + "', name='"
                    + super.name
                    + "', otherPaths="
                    + Arrays.toString(super.otherPaths)
                    + ", vid="
                    + super.vendorId
                    + ", pid="
                    + super.productId
                    + ", uniquePath='"
                    + uniquePath()
                    + "']";
        }
    }

    @JsonTypeName("PVCSICameraInfo")
    public static final class PVCSICameraInfo implements PVCameraInfo {
        public final String path;
        public final String baseName;

        @JsonCreator
        public PVCSICameraInfo(
                @JsonProperty("path") String path, @JsonProperty("baseName") String baseName) {
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
            if (obj instanceof PVCameraInfo info) {
                return equals(info);
            }
            return false;
        }

        @Override
        public String toString() {
            return "PVCsiCameraInfo[type="
                    + type()
                    + ", basename="
                    + baseName
                    + ", path='"
                    + path
                    + "', uniquePath='"
                    + uniquePath()
                    + "']";
        }
    }

    @JsonTypeName("PVFileCameraInfo")
    public static final class PVFileCameraInfo implements PVCameraInfo {
        public final String path;
        public final String name;

        @JsonCreator
        public PVFileCameraInfo(@JsonProperty("path") String path, @JsonProperty("name") String name) {
            this.path = path;
            this.name = name;
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
            return path();
        }

        @Override
        public String[] otherPaths() {
            return new String[0];
        }

        @Override
        public CameraType type() {
            return CameraType.FileCamera;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (obj instanceof PVFileCameraInfo info) {
                return equals(info);
            }
            return false;
        }

        @Override
        public String toString() {
            return "PVFileCameraInfo[type=" + type() + ", filename=" + name + ", path='" + path + "']";
        }
    }

    public static PVCameraInfo fromUsbCameraInfo(UsbCameraInfo info) {
        return new PVUsbCameraInfo(info);
    }

    public static PVCameraInfo fromCSICameraInfo(String path, String baseName) {
        return new PVCSICameraInfo(path, baseName);
    }

    public static PVCameraInfo fromFileInfo(String path, String baseName) {
        return new PVFileCameraInfo(path, baseName);
    }
}
