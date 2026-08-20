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
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.ImageRotationMode;
import org.photonvision.vision.opencv.Releasable;
import org.photonvision.vision.pipe.impl.HSVPipe;
import org.photonvision.vision.pipe.impl.StaticCropPipe;

public abstract class FrameProvider implements Supplier<Frame>, Releasable {
    protected int sequenceID = 0;

    /**
     * Smallest crop handed downstream, in pixels per axis. A sliver of an image is useless for vision
     * and some native detectors read out of bounds when given one -- apriltag segfaults on an image
     * only a few pixels tall -- so a smaller crop is grown back to this size.
     */
    private static final int MIN_CROP_DIMENSION = 16;

    // Escape hatch to allow us to synchronously (from the main vision thread) run
    // extra
    // setup/callbacks once cscore connects to our underlying device for the first
    // time
    public boolean cameraPropertiesCached = false;

    protected void onCameraConnected() {
        cameraPropertiesCached = true;
    }

    /** Internal provider for if the camera is currently connected. */
    protected abstract boolean checkCameraConnected();

    /** Checks if the camera is currently connected. Also handles connection events. */
    public boolean isConnected() {
        boolean connected = this.checkCameraConnected();

        if (!cameraPropertiesCached && connected) {
            onCameraConnected();
        }

        return connected;
    }

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

    /**
     * Ask the camera to statically crop the frames it outputs to the given rectangle. A null
     * rectangle disables cropping. The rectangle is in the coordinate space of the frame after any
     * rotation has been applied.
     */
    public abstract void requestFrameCrop(Rect cropRect);

    /**
     * Crop the given image in place to the pipe's currently configured rectangle.
     *
     * @return Whether the image was actually cropped (an empty image is left untouched).
     */
    protected static boolean cropInPlace(StaticCropPipe cropPipe, CVMat image) {
        var result = cropPipe.run(image);
        if (result.output == null) {
            return false;
        }

        // submat() returns a view into the parent buffer, so clone before copying back onto it.
        Mat cropped = result.output.getMat().clone();
        result.output.release();
        cropped.copyTo(image.getMat());
        cropped.release();
        return true;
    }

    /**
     * Clamp a requested crop rectangle to the bounds of an image of the given size, growing it to
     * {@link #MIN_CROP_DIMENSION} per axis if it is smaller than that.
     *
     * @return The clamped rectangle, or null if the crop is empty or would cover the entire image (in
     *     which case cropping is a no-op).
     */
    protected static Rect clampCropToImage(Rect cropRect, int imageCols, int imageRows) {
        if (cropRect == null || imageCols <= 0 || imageRows <= 0) {
            return null;
        }

        int x = Math.max(0, Math.min(cropRect.x, imageCols - 1));
        int y = Math.max(0, Math.min(cropRect.y, imageRows - 1));
        int width = Math.max(0, Math.min(cropRect.width, imageCols - x));
        int height = Math.max(0, Math.min(cropRect.height, imageRows - y));

        if (width <= 0 || height <= 0) {
            return null;
        }

        // Grow a too-small crop, then slide it back inside the image if growing pushed it off the edge.
        // An image smaller than the minimum can't be satisfied, so it caps out at the image itself.
        width = Math.min(Math.max(width, MIN_CROP_DIMENSION), imageCols);
        height = Math.min(Math.max(height, MIN_CROP_DIMENSION), imageRows);
        x = Math.min(x, imageCols - width);
        y = Math.min(y, imageRows - height);

        // A crop covering the entire image is a no-op; skip it to avoid needless copies.
        if (x == 0 && y == 0 && width == imageCols && height == imageRows) {
            return null;
        }

        return new Rect(x, y, width, height);
    }

    /** Ask the camera to provide either the input, output, or both frames. */
    public abstract void requestFrameCopies(boolean copyInput, boolean copyOutput);

    /** Ask the camera to rotate frames it outputs */
    public abstract void requestHsvSettings(HSVPipe.HSVParams params);

    /** Ask the camera to block for new frames (true) or use latest available (false) */
    public abstract void requestBlockForFrames(boolean blockForFrames);
}
