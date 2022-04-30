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

import edu.wpi.first.math.geometry.Rotation2d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.photonvision.common.util.math.MathUtils;
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
        this(image, MathUtils.wpiNanoTime(), frameStaticProperties);
    }

    public Frame() {
        this(
                new CVMat(),
                MathUtils.wpiNanoTime(),
                new FrameStaticProperties(0, 0, 0, new Rotation2d(), null));
    }

    public static Frame emptyFrame(int width, int height) {
        return new Frame(
                new CVMat(Mat.zeros(new Size(width, height), CvType.CV_8UC3)),
                MathUtils.wpiNanoTime(),
                new FrameStaticProperties(width, height, 0, new Rotation2d(), null));
    }

    public void copyTo(Frame destFrame) {
        image.getMat().copyTo(destFrame.image.getMat());
    }

    public static Frame copyFromAndRelease(Frame frame) {
        var mat = new CVMat();
        frame.image.copyTo(mat);
        frame.release();
        return new Frame(mat, frame.timestampNanos, frame.frameStaticProperties);
    }

    @Override
    public void release() {
        image.release();
    }
}
