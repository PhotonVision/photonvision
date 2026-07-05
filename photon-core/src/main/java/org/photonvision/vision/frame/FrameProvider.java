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
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.opencv.ImageRotationMode;
import org.photonvision.vision.opencv.Releasable;
import org.photonvision.vision.pipe.impl.HSVPipe;
import org.photonvision.vision.pipeline.FrameRecorder;

public abstract class FrameProvider implements Supplier<Frame>, Releasable {
    private static final Logger logger = new Logger(FrameProvider.class, LogGroup.Camera);

    protected int sequenceID = 0;

    // Escape hatch to allow us to synchronously (from the main vision thread) run
    // extra
    // setup/callbacks once cscore connects to our underlying device for the first
    // time
    public boolean cameraPropertiesCached = false;

    // volatile so the vision thread's unsynchronised read in USBFrameProvider.getInputMat sees
    // writes from the NT-listener-thread-driven setRecording / release without tearing.
    public volatile FrameRecorder frameRecorder = null;

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

    /** Ask the camera to provide either the input, output, or both frames. */
    public abstract void requestFrameCopies(boolean copyInput, boolean copyOutput);

    /** Ask the camera to rotate frames it outputs */
    public abstract void requestHsvSettings(HSVPipe.HSVParams params);

    /** Ask the camera to block for new frames (true) or use latest available (false) */
    public abstract void requestBlockForFrames(boolean blockForFrames);

    /**
     * Ask this provider to start/stop recording frames to disk. The base implementation does not
     * support recording: setRecording(true) is warn-logged and ignored. Overrides must NOT throw —
     * NTDataPublisher routes recordingRequest writes through this on the NT listener thread, with no
     * try/catch around the consumer, so an unchecked exception propagates into NT4's listener pool.
     */
    public void setRecording(boolean shouldRecord) {
        if (shouldRecord) {
            logger.warn("Ignoring setRecording(true): " + getName() + " does not support recording.");
        }
    }

    public boolean getRecording() {
        return false;
    }
}
