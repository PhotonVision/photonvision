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

public class UniqueCameraSummary {
    public final String uniqueName;
    public final String name;
    public final String path;
    public final String[] otherPaths;
    public final String type;

    public UniqueCameraSummary(
            String uniqueName, String name, String path, String[] otherPaths, CameraType type) {
        this.uniqueName = uniqueName;
        this.name = name;
        this.path = path;
        this.otherPaths = otherPaths;
        this.type = type.name();
    }

    public UniqueCameraSummary(String uniqueName, PVCameraDevice info) {
        this(uniqueName, info.name(), info.path(), info.otherPaths(), info.type());
    }
}
