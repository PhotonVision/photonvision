/*
 * Copyright (C) 2020 Photon Vision.
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

import org.opencv.core.Mat;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.Releasable;

public class Frame implements Releasable {
    public final long timestampNanos;
    public final CVMat image;
    public final FrameStaticProperties frameStaticProperties;

    public Frame(CVMat image, long timestampNanos, FrameStaticProperties frameStaticProperties) {
        this.image = image;
        this.timestampNanos = timestampNanos;
        this.frameStaticProperties = frameStaticProperties;
    }

    public Frame(CVMat image, FrameStaticProperties frameStaticProperties) {
        this(image, System.nanoTime(), frameStaticProperties);
    }

    public void copyTo(Mat destMat) {
        image.getMat().copyTo(destMat);
    }

    public static Frame copyFrom(Frame frame) {
        Mat newMat = new Mat();
        frame.image.getMat().copyTo(newMat);
        frame.release();
        return new Frame(new CVMat(newMat), frame.timestampNanos, frame.frameStaticProperties);
    }

    @Override
    public void release() {
        image.release();
    }
}
