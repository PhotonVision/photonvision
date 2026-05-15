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

package org.photonvision.vision.processes;

import java.nio.file.Path;
import java.util.Optional;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.provider.FileLogFrameProvider;
import org.photonvision.vision.opencv.Releasable;

public abstract class VisionSource implements Releasable {
    protected final CameraConfiguration cameraConfiguration;

    // Held volatile so a swap on a controller thread (e.g. a replay-trigger REST handler) is
    // visible to the vision thread on its next get() — VisionRunner re-reads through
    // getFrameProvider() every loop iteration, so the new provider takes effect on the next frame.
    private volatile FrameProvider frameProvider;

    protected VisionSource(CameraConfiguration cameraConfiguration) {
        this.cameraConfiguration = cameraConfiguration;
    }

    public CameraConfiguration getCameraConfiguration() {
        return cameraConfiguration;
    }

    public FrameProvider getFrameProvider() {
        return frameProvider;
    }

    /**
     * Atomically swap the active frame provider. Caller is responsible for the lifecycle of the
     * outgoing provider (release / pause / restore). The vision thread reads through this getter on
     * every loop tick, so the new provider takes effect on the next frame grab.
     */
    public void setFrameProvider(FrameProvider provider) {
        this.frameProvider = provider;
    }

    /**
     * @return the recording directory backing this source's current frame provider if it's a {@link
     *     FileLogFrameProvider}, empty otherwise. Used to gate replay-only code paths
     *     (JsonResultExporter tee, replay-progress publishing) on the runtime provider rather than
     *     the matched camera info — so an in-place USB-cam → file-log swap activates them
     *     transparently.
     */
    public final Optional<Path> getReplayRecordingDir() {
        var fp = this.frameProvider;
        return (fp instanceof FileLogFrameProvider flf)
                ? Optional.of(flf.getRecordingDir())
                : Optional.empty();
    }

    public abstract VisionSourceSettables getSettables();

    public abstract boolean isVendorCamera();

    public abstract boolean hasLEDs();

    public abstract void remakeSettables();
}
