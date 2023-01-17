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

import org.opencv.core.Mat;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.raspi.LibCameraJNI;
import org.photonvision.vision.camera.LibcameraGpuSettables;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.ImageRotationMode;
import org.photonvision.vision.pipe.impl.HSVPipe.HSVParams;

public class LibcameraGpuFrameProvider implements FrameProvider {
    private final LibcameraGpuSettables settables;

    public LibcameraGpuFrameProvider(LibcameraGpuSettables visionSettables) {
        this.settables = visionSettables;

        var vidMode = settables.getCurrentVideoMode();
        settables.setVideoMode(vidMode);
    }

    @Override
    public String getName() {
        return "AcceleratedPicamFrameProvider";
    }

    int i = 0;

    @Override
    public Frame get() {
        // We need to make sure that other threads don't try to change video modes while we're waiting
        // for a frame
        // System.out.println("GET!");
        synchronized (LibCameraJNI.CAMERA_LOCK) {
            var success = LibCameraJNI.awaitNewFrame();

            if (!success) {
                System.out.println("No new frame");
                return new Frame();
            }

            var colorMat = new CVMat(new Mat(LibCameraJNI.takeColorFrame()));
            var processedMat = new CVMat(new Mat(LibCameraJNI.takeProcessedFrame()));

            // System.out.println("Color mat: " + colorMat.getMat().size());

            // Imgcodecs.imwrite("color" + i + ".jpg", colorMat.getMat());
            // Imgcodecs.imwrite("processed" + (i) + ".jpg", processedMat.getMat());

            int itype = LibCameraJNI.getGpuProcessType();
            FrameThresholdType type = FrameThresholdType.NONE;
            if (itype < FrameThresholdType.values().length && itype >= 0) {
                type = FrameThresholdType.values()[itype];
            }

            var now = LibCameraJNI.getLibcameraTimestamp();
            var capture = LibCameraJNI.getFrameCaptureTime();
            var latency = (now - capture);

            return new Frame(
                    colorMat,
                    processedMat,
                    type,
                    MathUtils.wpiNanoTime() - latency,
                    settables.getFrameStaticProperties());
        }
    }

    @Override
    public void requestFrameThresholdType(FrameThresholdType type) {
        LibCameraJNI.setGpuProcessType(type.ordinal());
    }

    @Override
    public void requestFrameRotation(ImageRotationMode rotationMode) {
        this.settables.setRotation(rotationMode);
    }

    @Override
    public void requestHsvSettings(HSVParams params) {
        LibCameraJNI.setThresholds(
                params.getHsvLower().val[0] / 180.0,
                params.getHsvLower().val[1] / 255.0,
                params.getHsvLower().val[2] / 255.0,
                params.getHsvUpper().val[0] / 180.0,
                params.getHsvUpper().val[1] / 255.0,
                params.getHsvUpper().val[2] / 255.0,
                params.getHueInverted());
    }

    @Override
    public void requestFrameCopies(boolean copyInput, boolean copyOutput) {
        LibCameraJNI.setFramesToCopy(copyInput, copyOutput);
    }
}
