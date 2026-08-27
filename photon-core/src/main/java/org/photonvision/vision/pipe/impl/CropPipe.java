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

import java.util.Objects;
import java.util.Optional;
import org.opencv.core.Rect;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipe.CVPipe;
import org.photonvision.vision.pipeline.AdvancedPipelineSettings;
import org.photonvision.vision.pipeline.AprilTagPipelineSettings;

/**
 * The crop rectangle is rebuilt on every {@link #setParams} call (the settings object is mutated in
 * place elsewhere, so it must not be cached against), and clamped into each input image as it is
 * processed. The output is a view into the input.
 */
public class CropPipe extends CVPipe<CVMat, CVMat, CropPipe.CropPipeParams> {
    /**
     * Side of the square tiles apriltag thresholds the decimated image in. Snapping the crop origin
     * to this grid prevents crop's from changing reported pose.
     */
    private static final int APRILTAG_TILE_SIZE = 4;

    /** Smallest crop handed downstream, in pixels per axis, prevents downstream crashes. */
    private static final int MIN_CROP_DIMENSION = 16;

    /** The rectangle derived from the current params, before clamping to an image. */
    private Rect cropRect = null;

    /**
     * @param settings The pipeline settings the crop rectangle is derived from.
     * @param cropRegion When present, used as the crop range directly, overriding whatever the
     *     settings describe -- for crops to sizes determined in code rather than by the user.
     */
    public static record CropPipeParams(
            AdvancedPipelineSettings settings, Optional<Rect> cropRegion) {
        public CropPipeParams {
            Objects.requireNonNull(cropRegion, "cropRegion must be an Optional, not null");
        }

        public CropPipeParams(AdvancedPipelineSettings settings) {
            this(settings, Optional.empty());
        }
    }

    @Override
    public void setParams(CropPipeParams newParams) {
        this.cropRect =
                newParams
                        .cropRegion()
                        .map(region -> alignForAprilTag(region, newParams.settings()))
                        .orElseGet(
                                () ->
                                        newParams.settings() != null
                                                ? cropRectFromSettings(newParams.settings())
                                                : null);
        super.setParams(newParams);
    }

    /**
     * The crop rectangle that applies to an image of the given size: the configured region clamped
     * into the image. Null when cropping is disabled, the region is degenerate, or it covers the
     * whole image (all of which make cropping a no-op).
     */
    public Rect effectiveCrop(int imageCols, int imageRows) {
        return clampCropToImage(cropRect, imageCols, imageRows);
    }

    @Override
    protected CVMat process(CVMat in) {
        if (in.getMat().empty()) {
            return null;
        }

        Rect effective = effectiveCrop(in.getMat().cols(), in.getMat().rows());
        if (effective == null) {
            return null;
        }

        return new CVMat(in.getMat().submat(effective));
    }

    /**
     * Build the static crop rectangle from pipeline settings, or null if cropping is disabled or the
     * configured region is degenerate. The ranges are stored as [min, max] pixel couples.
     */
    public static Rect cropRectFromSettings(AdvancedPipelineSettings settings) {
        if (!settings.staticCropEnabled) {
            return null;
        }

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

        if (width <= 0 || height <= 0) {
            return null;
        }

        return alignForAprilTag(new Rect(xLow, yLow, width, height), settings);
    }

    /**
     * Snap a crop rectangle's origin down onto apriltag's threshold-tile grid when the settings
     * belong to an AprilTag pipeline, growing the region up to the tile boundary rather than moving
     * it, so the crop still covers everything that was asked for.
     */
    private static Rect alignForAprilTag(Rect rect, AdvancedPipelineSettings settings) {
        if (rect == null || !(settings instanceof AprilTagPipelineSettings tagSettings)) {
            return rect;
        }

        int tile = APRILTAG_TILE_SIZE * Math.max(1, tagSettings.decimate);
        int alignedX = (rect.x / tile) * tile;
        int alignedY = (rect.y / tile) * tile;

        return new Rect(
                alignedX, alignedY, rect.width + rect.x - alignedX, rect.height + rect.y - alignedY);
    }

    /**
     * Clamp a requested crop rectangle to the bounds of an image of the given size, growing it to
     * {@link #MIN_CROP_DIMENSION} per axis if it is smaller than that.
     *
     * @return The clamped rectangle, or null if the crop is empty or would cover the entire image (in
     *     which case cropping is a no-op).
     */
    public static Rect clampCropToImage(Rect cropRect, int imageCols, int imageRows) {
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

    @Override
    public void release() {}
}
