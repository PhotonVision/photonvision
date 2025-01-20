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
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.raspi.LibCameraJNI;
import org.photonvision.vision.camera.csi.LibcameraGpuSettables;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.ImageRotationMode;
import org.photonvision.vision.pipe.impl.HSVPipe.HSVParams;

public class LibcameraGpuFrameProvider extends FrameProvider {
    private final LibcameraGpuSettables settables;

    static final Logger logger = new Logger(LibcameraGpuFrameProvider.class, LogGroup.Camera);

    public LibcameraGpuFrameProvider(LibcameraGpuSettables visionSettables) {
        this.settables = visionSettables;

        var vidMode = settables.getCurrentVideoMode();
        settables.setVideoMode(vidMode);
        this.cameraPropertiesCached =
                true; // Camera properties are not able to be changed so they are always cached
    }

    @Override
    public String getName() {
        return "AcceleratedPicamFrameProvider";
    }

    int badFrameCounter = 0;

    @Override
    public Frame get() {
        // We need to make sure that other threads don't try to change video modes while
        // we're waiting for a frame
        // System.out.println("GET!");
        synchronized (settables.CAMERA_LOCK) {
            var p_ptr = LibCameraJNI.awaitNewFrame(settables.r_ptr);

            if (p_ptr == 0) {
                logger.error("No new frame from " + settables.getConfiguration().nickname);
                badFrameCounter++;
                if (badFrameCounter > 3) {
                    logger.error(
                            "No new frame from "
                                    + settables.getConfiguration().nickname
                                    + " for 3 seconds attempting recreate!");
                    settables.setVideoMode(settables.getCurrentVideoMode());
                    badFrameCounter = 0;
                }
                return new Frame();
            }
            badFrameCounter = 0;

            var colorMat = new CVMat(new Mat(LibCameraJNI.takeColorFrame(p_ptr)));
            var processedMat = new CVMat(new Mat(LibCameraJNI.takeProcessedFrame(p_ptr)));

            // System.out.println("Color mat: " + colorMat.getMat().size());

            // Imgcodecs.imwrite("color" + i + ".jpg", colorMat.getMat());
            // Imgcodecs.imwrite("processed" + (i) + ".jpg", processedMat.getMat());

            int itype = LibCameraJNI.getGpuProcessType(p_ptr);
            FrameThresholdType type = FrameThresholdType.NONE;
            if (itype < FrameThresholdType.values().length && itype >= 0) {
                type = FrameThresholdType.values()[itype];
            }

            var now = LibCameraJNI.getLibcameraTimestamp();
            var capture = LibCameraJNI.getFrameCaptureTime(p_ptr);
            var latency = (now - capture);

            LibCameraJNI.releasePair(p_ptr);

            // Know frame is good -- increment sequence
            ++sequenceID;

            return new Frame(
                    sequenceID,
                    colorMat,
                    processedMat,
                    type,
                    MathUtils.wpiNanoTime() - latency,
                    settables.getFrameStaticProperties().rotate(settables.getRotation()));
        }
    }

    @Override
    public void requestFrameThresholdType(FrameThresholdType type) {
        LibCameraJNI.setGpuProcessType(settables.r_ptr, type.ordinal());
    }

    @Override
    public void requestFrameRotation(ImageRotationMode rotationMode) {
        this.settables.setRotation(rotationMode);
    }

    @Override
    public void requestHsvSettings(HSVParams params) {
        LibCameraJNI.setThresholds(
                settables.r_ptr,
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
        LibCameraJNI.setFramesToCopy(settables.r_ptr, copyInput, copyOutput);
    }

    @Override
    public void release() {
        synchronized (settables.CAMERA_LOCK) {
            LibCameraJNI.stopCamera(settables.r_ptr);
            LibCameraJNI.destroyCamera(settables.r_ptr);
            settables.r_ptr = 0;
        }
    }

    @Override
    public boolean checkCameraConnected() {
        String[] cameraNames = LibCameraJNI.getCameraNames();
        for (String name : cameraNames) {
            if (name.equals(settables.getConfiguration().getDevicePath())) {
                return true;
            }
        }
        return false;
    }

    // To our knowledge the camera is always connected (after boot) with csi cameras
    @Override
    public boolean isConnected() {
        return checkCameraConnected();
    }
}
