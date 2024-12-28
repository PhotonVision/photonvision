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

import org.photonvision.common.util.OrderedTracer;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.Releasable;

public class Frame implements Releasable {
    public final long sequenceID;
    public final long timestampNanos;

    // Frame should at _least_ contain the thresholded frame, and sometimes the color image
    public final CVMat colorImage;
    public final CVMat processedImage;
    public final FrameThresholdType type;

    public final FrameStaticProperties frameStaticProperties;

    // A Frame owns the tracer from frame capture from the underlying device until publish/GC. This
    // does mean we get extra GC pressure, but w/e
    public final OrderedTracer m_tracer;

    public Frame(
            long sequenceID,
            CVMat color,
            CVMat processed,
            FrameThresholdType type,
            long timestampNanos,
            FrameStaticProperties frameStaticProperties,
            OrderedTracer tracer) {
        this.sequenceID = sequenceID;
        this.colorImage = color;
        this.processedImage = processed;
        this.type = type;
        this.timestampNanos = timestampNanos;
        this.frameStaticProperties = frameStaticProperties;
        this.m_tracer = tracer;
    }

    public Frame(
            long sequenceID,
            CVMat color,
            CVMat processed,
            FrameThresholdType processType,
            FrameStaticProperties frameStaticProperties,
            OrderedTracer tracer) {
        this(
                sequenceID,
                color,
                processed,
                processType,
                MathUtils.wpiNanoTime(),
                frameStaticProperties,
                tracer);
    }

    public Frame() {
        this(
                -1,
                new CVMat(),
                new CVMat(),
                FrameThresholdType.NONE,
                MathUtils.wpiNanoTime(),
                new FrameStaticProperties(0, 0, 0, null),
                new OrderedTracer());
    }

    public void copyTo(Frame destFrame) {
        colorImage.getMat().copyTo(destFrame.colorImage.getMat());
        processedImage.getMat().copyTo(destFrame.processedImage.getMat());
    }

    @Override
    public void release() {
        colorImage.release();
        processedImage.release();
    }
}
