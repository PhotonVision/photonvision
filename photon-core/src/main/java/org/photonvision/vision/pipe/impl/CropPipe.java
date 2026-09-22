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

import org.opencv.core.Rect;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipe.CVPipe;
import org.photonvision.vision.pipeline.AdvancedPipelineSettings;
import org.photonvision.vision.pipeline.AprilTagPipelineSettings;

/**
 * Crops an image to a requested rectangle. The params carry the rectangle with its origin already
 * aligned to the AprilTag detector's tile grid -- alignment runs when the params are constructed,
 * reading the settings at that moment, so a params object must be rebuilt (and re-handed to {@link
 * #setParams}) when settings mutate in place. Each input image clamps the rectangle into its own
 * bounds as it is processed. The output is a view into the input, or null when the crop is a no-op.
 */
public class CropPipe extends CVPipe<CVMat, CVMat, CropPipe.CropPipeParams> {
    /**
     * Side of the square tiles apriltag thresholds the decimated image in. Snapping the crop origin
     * to this grid prevents crop's from changing reported pose.
     */
    private static final int APRILTAG_TILE_SIZE = 4;

    /** Smallest crop handed downstream, in pixels per axis, prevents downstream crashes. */
    private static final int MIN_CROP_DIMENSION = 16;

    /**
     * @param rect The region to crop to, in frame coordinates, with its origin aligned to the
     *     AprilTag detector's tile grid; null means no crop. The stored value is the aligned region
     *     the pipe crops to, not necessarily the rect passed in.
     * @param settings The pipeline settings the crop serves; read at construction, so a params object
     *     does not follow later in-place mutation of the settings.
     */
    public static record CropPipeParams(Rect rect, AdvancedPipelineSettings settings) {
        public CropPipeParams {
            rect = alignToTagTiles(rect, settings);
        }

        public CropPipeParams(AdvancedPipelineSettings settings) {
            // A pixel bound is never negative. Dropping the sign rather than trusting it keeps a garbage
            // bound (a value that overflowed on its way in, say) from being read as a sliver of a crop
            // one pixel from the origin.
            int xLow =
                    Math.max(0, Math.min(settings.staticCropX.getFirst(), settings.staticCropX.getSecond()));
            int xHigh =
                    Math.max(0, Math.max(settings.staticCropX.getFirst(), settings.staticCropX.getSecond()));
            int yLow =
                    Math.max(0, Math.min(settings.staticCropY.getFirst(), settings.staticCropY.getSecond()));
            int yHigh =
                    Math.max(0, Math.max(settings.staticCropY.getFirst(), settings.staticCropY.getSecond()));

            int width = xHigh - xLow;
            int height = yHigh - yLow;

            Rect cropRegion;
            if (width <= 0 || height <= 0) {
                cropRegion = null;
            } else {
                cropRegion = new Rect(xLow, yLow, width, height);
            }

            if (!settings.staticCropEnabled) {
                cropRegion = null;
            }

            this(cropRegion, settings);
        }
    }

    @Override
    protected CVMat process(CVMat in) {
        if (in.getMat().empty()) {
            return null;
        }

        Rect effective = clampCropToImage(params.rect(), in.getMat().cols(), in.getMat().rows());
        if (effective == null) {
            return null;
        }

        return new CVMat(in.getMat().submat(effective));
    }

    // Cropping calibrated frame static properties derives fresh calibration coefficients that hold
    // native memory, and neither the source properties nor the crop rectangle changes frame to
    // frame -- so cache the last derivation and release it once it is superseded.
    private FrameStaticProperties cachedSourceProperties = null;
    private Rect cachedCropRect = null;
    private FrameStaticProperties cachedCroppedProperties = null;

    /**
     * Frame static properties describing the given source properties cropped to the given rectangle,
     * cached against both.
     *
     * @param source The uncropped frame's properties.
     * @param cropRect The crop rectangle applied to the frame.
     * @return The cropped properties. Owned by this pipe: released when the crop changes, so callers
     *     must not hold them across frames.
     */
    public FrameStaticProperties croppedProperties(FrameStaticProperties source, Rect cropRect) {
        if (source != cachedSourceProperties || !cropRect.equals(cachedCropRect)) {
            releaseCachedProperties();
            cachedCroppedProperties = source.crop(cropRect);
            cachedSourceProperties = source;
            cachedCropRect = cropRect.clone();
        }
        return cachedCroppedProperties;
    }

    /**
     * Discard the cached cropped properties, releasing the derived calibration coefficients they own.
     * Call when the crop becomes a no-op, so the cache does not hold native memory alive until a crop
     * happens to come along again.
     */
    public void releaseCachedProperties() {
        if (cachedCroppedProperties != null
                && cachedCroppedProperties.cameraCalibration != null
                // Only release coefficients the crop derived -- never the ones borrowed from the
                // camera, which outlive any single crop.
                && (cachedSourceProperties == null
                        || cachedCroppedProperties.cameraCalibration
                                != cachedSourceProperties.cameraCalibration)) {
            cachedCroppedProperties.cameraCalibration.release();
        }

        cachedSourceProperties = null;
        cachedCropRect = null;
        cachedCroppedProperties = null;
    }

    private static Rect alignToTagTiles(Rect rect, AdvancedPipelineSettings settings) {
        if (rect == null || !(settings instanceof AprilTagPipelineSettings tagSettings)) {
            return rect;
        }

        // An ML bounding box padded past the frame edge can carry a negative origin; pull it to 0
        // (shrinking the region by the overhang, as clamping would) before aligning.
        int xLow = Math.max(0, rect.x);
        int yLow = Math.max(0, rect.y);
        int width = rect.width - (xLow - rect.x);
        int height = rect.height - (yLow - rect.y);

        if (width <= 0 || height <= 0) {
            return null;
        }

        int tile = APRILTAG_TILE_SIZE * tagSettings.decimate;
        // Snap the crop origin outward to the nearest tile boundary below it. If the low bound
        // is already at 0 (touching the left/top edge), keep it at 0 rather than moving it
        // to a value computed from the high bound (which could overflow past the image and
        // produce negative widths).
        int alignedX = xLow - (xLow % tile);
        int alignedY = yLow - (yLow % tile);

        width += xLow - alignedX;
        height += yLow - alignedY;

        return new Rect(alignedX, alignedY, width, height);
    }

    /**
     * Clamp a requested crop rectangle to the bounds of an image of the given size, growing it to
     * {@link #MIN_CROP_DIMENSION} per axis if it is smaller than that.
     *
     * @param cropRect The requested crop rectangle; may be null for no crop.
     * @param imageCols The image's width, in pixels.
     * @param imageRows The image's height, in pixels.
     * @return The clamped rectangle, or null if the crop is empty or would cover the entire image (in
     *     which case cropping is a no-op).
     */
    public static Rect clampCropToImage(Rect cropRect, int imageCols, int imageRows) {
        if (cropRect == null || imageCols <= 0 || imageRows <= 0) {
            return null;
        }

        int x = Math.clamp(cropRect.x, 0, imageCols - 1);
        int y = Math.clamp(cropRect.y, 0, imageRows - 1);
        int width = Math.clamp(cropRect.width, 0, imageCols - x);
        int height = Math.clamp(cropRect.height, 0, imageRows - y);

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

    @Override
    public void release() {
        releaseCachedProperties();
    }
}
