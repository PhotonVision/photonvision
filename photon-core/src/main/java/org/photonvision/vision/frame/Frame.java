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

import java.util.concurrent.atomic.AtomicInteger;

import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.Releasable;

public class Frame implements Releasable {
    private static final Logger logger = new Logger(Frame.class, LogGroup.General);

    public final long sequenceID;
    public final long timestampNanos;

    // Frame should at _least_ contain the thresholded frame, and sometimes the color image
    public final CVMat colorImage;
    public final CVMat processedImage;
    public final FrameThresholdType type;

    public final FrameStaticProperties frameStaticProperties;

    public Frame(
            long sequenceID,
            CVMat color,
            CVMat processed,
            FrameThresholdType type,
            long timestampNanos,
            FrameStaticProperties frameStaticProperties) {
        this.sequenceID = sequenceID;
        this.colorImage = color;
        this.processedImage = processed;
        this.type = type;
        this.timestampNanos = timestampNanos;
        this.frameStaticProperties = frameStaticProperties;

        logger.trace(() -> "Allocated Frame " + sequenceID + "; color image " + colorImage.matId + "; processed " + processedImage.matId);
    }

    public Frame(
            long sequenceID,
            CVMat color,
            CVMat processed,
            FrameThresholdType processType,
            FrameStaticProperties frameStaticProperties) {
        this(sequenceID, color, processed, processType, MathUtils.wpiNanoTime(), frameStaticProperties);
    }

    public Frame() {
        this(
                -1,
                new CVMat(),
                new CVMat(),
                FrameThresholdType.NONE,
                MathUtils.wpiNanoTime(),
                new FrameStaticProperties(0, 0, 0, null));
    }

    public void copyTo(Frame destFrame) {
        colorImage.getMat().copyTo(destFrame.colorImage.getMat());
        processedImage.getMat().copyTo(destFrame.processedImage.getMat());
    }

    @Override
    public void release() {
        logger.trace(() -> "Releasing Frame " + sequenceID + "; color image " + colorImage.matId + "; processed " + processedImage.matId);

        colorImage.release();
        processedImage.release();
    }
}
