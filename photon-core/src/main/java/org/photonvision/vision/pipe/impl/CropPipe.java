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

public class CropPipe extends CVPipe<CVMat, CVMat, CropPipe.CropPipeParams> {
    private static final int APRILTAG_TILE_SIZE = 4;
    private static final int MIN_CROP_DIMENSION = 16;

    /**
     * Parametres for the crop pipe. Automatically aligns the rectangle to the apriltag detector
     * tiles.
     */
    public static record CropPipeParams(Rect rect, AdvancedPipelineSettings settings) {
        public CropPipeParams {
            rect = alignToTagTiles(rect, settings);
        }

        public CropPipeParams(AdvancedPipelineSettings settings) {
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

    private FrameStaticProperties cachedSourceProperties = null;
    private Rect cachedCropRect = null;
    private FrameStaticProperties cachedCroppedProperties = null;

    public FrameStaticProperties croppedProperties(FrameStaticProperties source, Rect cropRect) {
        if (source != cachedSourceProperties || !cropRect.equals(cachedCropRect)) {
            releaseCachedProperties();
            cachedCroppedProperties = source.crop(cropRect);
            cachedSourceProperties = source;
            cachedCropRect = cropRect.clone();
        }
        return cachedCroppedProperties;
    }

    public void releaseCachedProperties() {
        if (cachedCroppedProperties != null
                && cachedCroppedProperties.cameraCalibration != null
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

        int xLow = Math.max(0, rect.x);
        int yLow = Math.max(0, rect.y);
        int width = rect.width - (xLow - rect.x);
        int height = rect.height - (yLow - rect.y);

        if (width <= 0 || height <= 0) {
            return null;
        }

        int tile = APRILTAG_TILE_SIZE * tagSettings.decimate;

        // Snap the crop origin outward to the nearest tile boundary below it.
        int alignedX = xLow - (xLow % tile);
        int alignedY = yLow - (yLow % tile);

        width += xLow - alignedX;
        height += yLow - alignedY;

        return new Rect(alignedX, alignedY, width, height);
    }

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

        width = Math.min(Math.max(width, MIN_CROP_DIMENSION), imageCols);
        height = Math.min(Math.max(height, MIN_CROP_DIMENSION), imageRows);
        x = Math.min(x, imageCols - width);
        y = Math.min(y, imageRows - height);

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
