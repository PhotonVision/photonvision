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

import java.util.*;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.opencv.ImageRotationMode;
import org.photonvision.vision.pipe.impl.HSVPipe.HSVParams;
import org.photonvision.vision.processes.VisionSource;
import org.photonvision.vision.processes.VisionSourceSettables;

/** Dummy class for unit testing the vision source manager */
public class TestSource extends VisionSource {
    private FrameProvider usbFrameProvider;

    public TestSource(CameraConfiguration config) {
        super(config);

        if (getCameraConfiguration().cameraQuirks == null)
            getCameraConfiguration().cameraQuirks =
                    QuirkyCamera.getQuirkyCamera(config.usbVID, config.usbVID, config.baseName);
    }

    @Override
    public FrameProvider getFrameProvider() {
        return new FrameProvider() {
            @Override
            public Frame get() {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'get'");
            }

            @Override
            public String getName() {
                return cameraConfiguration.uniqueName;
            }

            @Override
            public void requestFrameThresholdType(FrameThresholdType type) {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'requestFrameThresholdType'");
            }

            @Override
            public void requestFrameRotation(ImageRotationMode rotationMode) {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'requestFrameRotation'");
            }

            @Override
            public void requestFrameCopies(boolean copyInput, boolean copyOutput) {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'requestFrameCopies'");
            }

            @Override
            public void requestHsvSettings(HSVParams params) {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'requestHsvSettings'");
            }
        };
    }

    @Override
    public VisionSourceSettables getSettables() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSettables'");
    }

    @Override
    public boolean isVendorCamera() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isVendorCamera'");
    }
}
