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

package org.photonvision.vision.frame.provider;

import org.opencv.core.Rect;
import org.photonvision.common.util.numbers.IntegerCouple;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.ImageRotationMode;
import org.photonvision.vision.pipe.impl.GrayscalePipe;
import org.photonvision.vision.pipe.impl.HSVPipe;
import org.photonvision.vision.pipe.impl.RotateImagePipe;
import org.photonvision.vision.pipe.impl.StaticCropPipe;

public abstract class CpuImageProcessor extends FrameProvider {
    protected static class CapturedFrame {
        CVMat colorImage;
        FrameStaticProperties staticProps;
        long captureTimestamp;

        public CapturedFrame(
                CVMat colorImage, FrameStaticProperties staticProps, long captureTimestampNanos) {
            this.colorImage = colorImage;
            this.staticProps = staticProps;
            this.captureTimestamp = captureTimestampNanos;
        }
    }

    private final HSVPipe m_hsvPipe = new HSVPipe();
    private final RotateImagePipe m_rImagePipe = new RotateImagePipe();
    private final GrayscalePipe m_grayPipe = new GrayscalePipe();
    private final StaticCropPipe m_cropPipe = new StaticCropPipe();
    FrameThresholdType m_processType;
    // The requested crop, in the coordinate space of the rotated frame. Null disables cropping.
    private Rect m_cropRect = null;
    boolean m_blockForFrames = true;

    private final Object m_mutex = new Object();

    abstract CapturedFrame getInputMat();

    public CpuImageProcessor() {
        m_hsvPipe.setParams(
                new HSVPipe.HSVParams(
                        new IntegerCouple(0, 180),
                        new IntegerCouple(0, 255),
                        new IntegerCouple(0, 255),
                        false));
    }

    @Override
    public final Frame get() {
        var input = getInputMat();

        m_rImagePipe.run(input.colorImage.getMat());

        // Statically crop the (already-rotated) color image so all downstream processing, drawing,
        // and streaming operate on the cropped region. The crop is clamped to the current frame size.
        Rect effectiveCrop =
                clampCropToImage(
                        m_cropRect, input.colorImage.getMat().cols(), input.colorImage.getMat().rows());
        if (effectiveCrop != null) {
            m_cropPipe.setParams(effectiveCrop);
            if (!cropInPlace(m_cropPipe, input.colorImage)) {
                effectiveCrop = null;
            }
        }

        CVMat outputMat = null;
        if (!input.colorImage.getMat().empty()) {
            if (m_processType == FrameThresholdType.HSV) {
                var hsvResult = m_hsvPipe.run(input.colorImage.getMat());
                outputMat = new CVMat(hsvResult.output);
            } else if (m_processType == FrameThresholdType.GREYSCALE) {
                var result = m_grayPipe.run(input.colorImage.getMat());
                outputMat = new CVMat(result.output);
            } else {
                outputMat = new CVMat();
            }

            ++sequenceID;
        } else {
            System.out.println("Input was empty!");
            outputMat = new CVMat();
        }

        return new Frame(
                sequenceID,
                input.colorImage,
                outputMat,
                m_processType,
                input.captureTimestamp,
                input.staticProps != null
                        ? input.staticProps.rotate(m_rImagePipe.getParams().rotation()).crop(effectiveCrop)
                        : input.staticProps);
    }

    @Override
    public void requestFrameThresholdType(FrameThresholdType type) {
        synchronized (m_mutex) {
            this.m_processType = type;
        }
    }

    @Override
    public void requestFrameRotation(ImageRotationMode rotationMode) {
        synchronized (m_mutex) {
            m_rImagePipe.setParams(new RotateImagePipe.RotateImageParams(rotationMode));
        }
    }

    @Override
    public void requestFrameCrop(Rect cropRect) {
        synchronized (m_mutex) {
            m_cropRect = cropRect;
        }
    }

    /** Ask the camera to rotate frames it outputs */
    public void requestHsvSettings(HSVPipe.HSVParams params) {
        synchronized (m_mutex) {
            m_hsvPipe.setParams(params);
        }
    }

    @Override
    public void requestFrameCopies(boolean copyInput, boolean copyOutput) {
        // We don't actually do zero-copy, so this method is a no-op
    }

    @Override
    public void requestBlockForFrames(boolean blockForFrames) {
        synchronized (m_mutex) {
            this.m_blockForFrames = blockForFrames;
        }
    }
}
