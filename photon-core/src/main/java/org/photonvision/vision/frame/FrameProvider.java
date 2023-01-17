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
import org.photonvision.vision.pipe.impl.HSVPipe;

public interface FrameProvider extends Supplier<Frame> {
    String getName();

    /** Ask the camera to produce a certain kind of processed image (eg HSV or greyscale) */
    public void requestFrameThresholdType(FrameThresholdType type);

    /** Ask the camera to rotate frames it outputs */
    public void requestFrameRotation(ImageRotationMode rotationMode);

    /** Ask the camera to provide either the input, output, or both frames. */
    public void requestFrameCopies(boolean copyInput, boolean copyOutput);

    /** Ask the camera to rotate frames it outputs */
    public void requestHsvSettings(HSVPipe.HSVParams params);
}
