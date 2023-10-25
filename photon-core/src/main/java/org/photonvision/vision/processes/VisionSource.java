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

package org.photonvision.vision.processes;

import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.vision.frame.FrameProvider;

public abstract class VisionSource {
    protected final CameraConfiguration cameraConfiguration;

    protected VisionSource(CameraConfiguration cameraConfiguration) {
        this.cameraConfiguration = cameraConfiguration;
    }

    public CameraConfiguration getCameraConfiguration() {
        return cameraConfiguration;
    }

    public abstract FrameProvider getFrameProvider();

    public abstract VisionSourceSettables getSettables();

    public abstract boolean isVendorCamera();
}
