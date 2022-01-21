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

package org.photonvision.vision.pipe.impl;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.photonvision.vision.opencv.ImageRotationMode;
import org.photonvision.vision.pipe.MutatingPipe;

/** Pipe that rotates an image to a given orientation */
public class RotateImagePipe extends MutatingPipe<Mat, RotateImagePipe.RotateImageParams> {
    public RotateImagePipe() {
        setParams(RotateImageParams.DEFAULT);
    }

    public RotateImagePipe(RotateImageParams params) {
        setParams(params);
    }

    /**
     * Process this pipe
     *
     * @param in {@link Mat} to be rotated
     * @return Rotated {@link Mat}
     */
    @Override
    protected Void process(Mat in) {
        Core.rotate(in, in, params.rotation.value);
        return null;
    }

    public static class RotateImageParams {
        public static RotateImageParams DEFAULT = new RotateImageParams(ImageRotationMode.DEG_0);

        public ImageRotationMode rotation;

        public RotateImageParams() {
            rotation = DEFAULT.rotation;
        }

        public RotateImageParams(ImageRotationMode rotation) {
            this.rotation = rotation;
        }
    }
}
