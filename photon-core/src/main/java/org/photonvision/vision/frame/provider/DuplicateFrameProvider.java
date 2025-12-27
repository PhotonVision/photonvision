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

import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.opencv.ImageRotationMode;
import org.photonvision.vision.pipe.impl.HSVPipe;

/**
 * A {@link FrameProvider} that wraps another FrameProvider to share frames with a source camera.
 * This allows duplicate cameras to process the same frames through different pipelines without
 * duplicating frame acquisition.
 *
 * <p>Frame acquisition is delegated to the source provider, but pre-processing (rotation,
 * threshold, HSV) is done independently for each duplicate based on its own pipeline settings.
 */
public class DuplicateFrameProvider extends FrameProvider {
    private final FrameProvider sourceFrameProvider;
    private final String duplicateName;
    private final Logger logger;

    /**
     * Create a new DuplicateFrameProvider that wraps a source provider.
     *
     * @param sourceFrameProvider The source camera's frame provider to delegate to
     * @param duplicateName The name of this duplicate camera for logging
     */
    public DuplicateFrameProvider(FrameProvider sourceFrameProvider, String duplicateName) {
        this.sourceFrameProvider = sourceFrameProvider;
        this.duplicateName = duplicateName;
        this.logger = new Logger(DuplicateFrameProvider.class, duplicateName, LogGroup.Camera);
    }

    /**
     * Get the next frame from the source camera. Returns the same frame instance as the source. Each
     * duplicate camera's pipeline will process the frame independently with its own settings.
     */
    @Override
    public Frame get() {
        return sourceFrameProvider.get();
    }

    @Override
    public boolean isConnected() {
        return sourceFrameProvider.isConnected();
    }

    @Override
    public boolean checkCameraConnected() {
        return sourceFrameProvider.checkCameraConnected();
    }

    @Override
    public boolean hasConnected() {
        return sourceFrameProvider.hasConnected();
    }

    @Override
    public String getName() {
        return duplicateName;
    }

    /**
     * Duplicate cameras cannot change the frame threshold type - this is controlled by the source
     * camera. Pipeline processing settings (blur, decimate, etc.) are independent per duplicate.
     */
    @Override
    public void requestFrameThresholdType(FrameThresholdType type) {
        // No-op: Pre-processing is done by source, but pipeline settings are independent
    }

    /**
     * Duplicate cameras cannot change the frame rotation - this is controlled by the source camera.
     */
    @Override
    public void requestFrameRotation(ImageRotationMode rotationMode) {
        // No-op: Pre-processing is done by source
    }

    /** Request frame copies for input/output. */
    @Override
    public void requestFrameCopies(boolean copyInput, boolean copyOutput) {
        sourceFrameProvider.requestFrameCopies(copyInput, copyOutput);
    }

    /** Duplicate cameras cannot change HSV settings - this is controlled by the source camera. */
    @Override
    public void requestHsvSettings(HSVPipe.HSVParams params) {
        // No-op: Pre-processing is done by source
    }

    /**
     * Release resources for this duplicate. Does NOT release the source provider as other cameras may
     * still be using it.
     */
    @Override
    public void release() {
        // Do not release the source provider - only clean up our own resources
        // Other VisionModules may still be using the source
        logger.debug("Releasing duplicate frame provider for " + duplicateName);
    }
}
