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

package org.photonvision.vision.camera;

import java.io.IOException;
import java.nio.file.Path;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.frame.provider.FileLogFrameProvider;
import org.photonvision.vision.processes.VisionSource;
import org.photonvision.vision.processes.VisionSourceSettables;

/**
 * VisionSource backed by a {@link FileLogFrameProvider} — replays a recording directory
 * (frames/ + metadata.jsonl) as if it were a live camera. Parallel to {@link
 * FileVisionSource}; kept separate because the lifecycles diverge (FileVisionSource loops a
 * single image, this one walks through a timed recording and exhausts).
 */
public class FileLogVisionSource extends VisionSource {
    private final FileLogFrameProvider frameProvider;
    private final FileLogSourceSettables settables;

    public FileLogVisionSource(CameraConfiguration cameraConfiguration) {
        super(cameraConfiguration);

        try {
            this.frameProvider =
                    new FileLogFrameProvider(Path.of(cameraConfiguration.getDevicePath()));
        } catch (IOException e) {
            // VisionSourceManager.loadVisionSourceFromCamConfig has no throws clause and the
            // existing switch arms don't propagate checked exceptions either. Wrap so the
            // failure surfaces in the same shape as other source-construction errors
            // (e.g. a CSI source whose device is unplugged).
            throw new RuntimeException(
                    "Failed to open replay source at " + cameraConfiguration.getDevicePath(), e);
        }

        if (getCameraConfiguration().cameraQuirks == null) {
            getCameraConfiguration().cameraQuirks = QuirkyCamera.DefaultCamera;
        }

        this.settables =
                new FileLogSourceSettables(cameraConfiguration, frameProvider.getStaticProperties());
    }

    @Override
    public FrameProvider getFrameProvider() {
        return frameProvider;
    }

    @Override
    public VisionSourceSettables getSettables() {
        return settables;
    }

    @Override
    public boolean isVendorCamera() {
        return false;
    }

    @Override
    public boolean hasLEDs() {
        return false;
    }

    @Override
    public void remakeSettables() {
        // Dimensions are baked in at construction from the mp4 header; never need to rebuild.
    }

    @Override
    public void release() {
        frameProvider.release();
    }

    /**
     * Settables for a replay source: every camera control is baked into the recording and
     * therefore a no-op. The shape is identical to {@link FileVisionSource.FileSourceSettables}
     * (which serves the same purpose for the single-image source), so extend it rather than
     * duplicating ~80 lines of no-op stubs. Any future drift in either source's settable
     * semantics should subclass-and-override; identical no-op behaviour stays shared.
     */
    public static class FileLogSourceSettables extends FileVisionSource.FileSourceSettables {
        FileLogSourceSettables(
                CameraConfiguration cameraConfiguration, FrameStaticProperties frameStaticProperties) {
            super(cameraConfiguration, frameStaticProperties);
        }
    }
}
