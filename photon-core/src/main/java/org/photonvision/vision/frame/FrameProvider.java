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

package org.photonvision.vision.frame;

import java.util.function.Supplier;
import org.photonvision.vision.opencv.ImageRotationMode;
import org.photonvision.vision.opencv.Releasable;
import org.photonvision.vision.pipe.impl.HSVPipe;

public abstract class FrameProvider implements Supplier<Frame>, Releasable {
    protected int sequenceID = 0;

    // Escape hatch to allow us to synchronously (from the main vision thread) run
    // extra
    // setup/callbacks once cscore connects to our underlying device for the first
    // time
    public boolean cameraPropertiesCached = false;

    protected void onCameraConnected() {
        cameraPropertiesCached = true;
    }

    public abstract boolean isConnected();

    public abstract boolean checkCameraConnected();

    /**
     * Returns if the camera has connected at some point. This is not if it is currently connected.
     */
    public boolean hasConnected() {
        return cameraPropertiesCached;
    }

    public abstract String getName();

    /** Ask the camera to produce a certain kind of processed image (e.g. HSV or greyscale) */
    public abstract void requestFrameThresholdType(FrameThresholdType type);

    /** Ask the camera to rotate frames it outputs */
    public abstract void requestFrameRotation(ImageRotationMode rotationMode);

    /** Ask the camera to provide either the input, output, or both frames. */
    public abstract void requestFrameCopies(boolean copyInput, boolean copyOutput);

    /** Ask the camera to rotate frames it outputs */
    public abstract void requestHsvSettings(HSVPipe.HSVParams params);
}
