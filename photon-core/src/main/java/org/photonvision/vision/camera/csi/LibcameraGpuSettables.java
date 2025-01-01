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

package org.photonvision.vision.camera.csi;

import edu.wpi.first.cscore.VideoMode;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.Pair;
import edu.wpi.first.util.PixelFormat;
import java.util.HashMap;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.raspi.LibCameraJNI;
import org.photonvision.vision.camera.csi.LibcameraGpuSource.FPSRatedVideoMode;
import org.photonvision.vision.opencv.ImageRotationMode;
import org.photonvision.vision.processes.VisionSourceSettables;

public class LibcameraGpuSettables extends VisionSourceSettables {
    private FPSRatedVideoMode currentVideoMode;
    private double lastManualExposure = 50;
    private int lastBrightness = 50;
    private boolean lastAutoExposureActive;
    private int lastGain = 50;
    private Pair<Integer, Integer> lastAwbGains = new Pair<>(18, 18);
    public long r_ptr = 0;

    private final LibCameraJNI.SensorModel sensorModel;

    private double minExposure = 1;
    private double maxExposure = 80000;

    private ImageRotationMode m_rotationMode = ImageRotationMode.DEG_0;

    public final Object CAMERA_LOCK = new Object();

    public void setRotation(ImageRotationMode rotationMode) {
        if (rotationMode != m_rotationMode) {
            m_rotationMode = rotationMode;

            setVideoModeInternal(getCurrentVideoMode());
        }
    }

    public ImageRotationMode getRotation() {
        return m_rotationMode;
    }

    public LibcameraGpuSettables(CameraConfiguration configuration) {
        super(configuration);

        videoModes = new HashMap<>();

        sensorModel = LibCameraJNI.getSensorModel(configuration.matchedCameraInfo.path());

        if (sensorModel == LibCameraJNI.SensorModel.IMX219) {
            // Settings for the IMX219 sensor, which is used on the Pi Camera Module v2
            videoModes.put(0, new FPSRatedVideoMode(PixelFormat.kUnknown, 320, 240, 120, 120, .39));
            videoModes.put(1, new FPSRatedVideoMode(PixelFormat.kUnknown, 320, 240, 30, 30, .39));
            videoModes.put(2, new FPSRatedVideoMode(PixelFormat.kUnknown, 640, 480, 65, 90, .39));
            videoModes.put(3, new FPSRatedVideoMode(PixelFormat.kUnknown, 640, 480, 30, 30, .39));
            // TODO: fix 1280x720 in the native code and re-add it
            videoModes.put(4, new FPSRatedVideoMode(PixelFormat.kUnknown, 1920, 1080, 15, 20, .53));
            videoModes.put(5, new FPSRatedVideoMode(PixelFormat.kUnknown, 3280 / 2, 2464 / 2, 15, 20, 1));
            videoModes.put(6, new FPSRatedVideoMode(PixelFormat.kUnknown, 3280 / 4, 2464 / 4, 15, 20, 1));
        } else if (sensorModel == LibCameraJNI.SensorModel.OV9281) {
            videoModes.put(0, new FPSRatedVideoMode(PixelFormat.kUnknown, 320, 240, 30, 30, .39));
            videoModes.put(1, new FPSRatedVideoMode(PixelFormat.kUnknown, 1280 / 2, 800 / 2, 60, 60, 1));
            videoModes.put(2, new FPSRatedVideoMode(PixelFormat.kUnknown, 640, 480, 65, 90, .39));
            videoModes.put(3, new FPSRatedVideoMode(PixelFormat.kUnknown, 1280, 800, 60, 60, 1));

        } else {
            if (sensorModel == LibCameraJNI.SensorModel.IMX477) {
                LibcameraGpuSource.logger.warn(
                        "It appears you are using a Pi HQ Camera. This camera is not officially supported. You will have to set your camera FOV differently based on resolution.");
            } else if (sensorModel == LibCameraJNI.SensorModel.IMX708) {
                LibcameraGpuSource.logger.warn(
                        "It appears you are using a Pi Camera V3. This camera is not officially supported. You will have to set your camera FOV differently based on resolution.");
            } else if (sensorModel == LibCameraJNI.SensorModel.Unknown) {
                LibcameraGpuSource.logger.warn(
                        "You have an unknown sensor connected to your Pi over CSI! This is likely a bug. If it is not, then you will have to set your camera FOV differently based on resolution.");
            }

            // Settings for the OV5647 sensor, which is used by the Pi Camera Module v1
            videoModes.put(0, new FPSRatedVideoMode(PixelFormat.kUnknown, 320, 240, 90, 90, 1));
            videoModes.put(1, new FPSRatedVideoMode(PixelFormat.kUnknown, 640, 480, 85, 90, 1));
            videoModes.put(2, new FPSRatedVideoMode(PixelFormat.kUnknown, 960, 720, 45, 49, 0.74));
            // Half the size of the active areas on the OV5647
            videoModes.put(3, new FPSRatedVideoMode(PixelFormat.kUnknown, 2592 / 2, 1944 / 2, 20, 20, 1));
            videoModes.put(4, new FPSRatedVideoMode(PixelFormat.kUnknown, 1280, 720, 30, 45, 0.91));
            videoModes.put(5, new FPSRatedVideoMode(PixelFormat.kUnknown, 1920, 1080, 15, 20, 0.72));
        }

        // TODO need to add more video modes for new sensors here

        currentVideoMode = (FPSRatedVideoMode) videoModes.get(0);

        if (sensorModel == LibCameraJNI.SensorModel.OV9281) {
            minExposure = 7;
        } else if (sensorModel == LibCameraJNI.SensorModel.OV5647) {
            minExposure = 560;
        }
        this.cameraPropertiesCached =
                true; // Camera properties are not able to be changed so they are always cached
    }

    @Override
    public double getFOV() {
        return getCurrentVideoMode().fovMultiplier * getConfiguration().FOV;
    }

    @Override
    public void setAutoExposure(boolean cameraAutoExposure) {
        logger.debug("Setting auto exposure to " + cameraAutoExposure);
        lastAutoExposureActive = cameraAutoExposure;
        LibCameraJNI.setAutoExposure(r_ptr, cameraAutoExposure);
        if (!cameraAutoExposure) {
            setExposureRaw(lastManualExposure);
        }
    }

    @Override
    public void setExposureRaw(double exposureRaw) {
        logger.debug("Setting exposure to " + exposureRaw);

        LibCameraJNI.setAutoExposure(r_ptr, false);

        // Store the exposure for use when we need to recreate the camera.
        lastManualExposure = exposureRaw;

        // 80,000 uS seems like an exposure value that will be greater than ever needed while giving
        // enough control over exposure.
        exposureRaw = MathUtil.clamp(exposureRaw, minExposure, maxExposure);

        var success = LibCameraJNI.setExposure(r_ptr, (int) exposureRaw);
        if (!success) LibcameraGpuSource.logger.warn("Couldn't set Pi Camera exposure");
    }

    @Override
    public void setBrightness(int brightness) {
        lastBrightness = brightness;
        double realBrightness = MathUtils.map(brightness, 0.0, 100.0, -1.0, 1.0);
        var success = LibCameraJNI.setBrightness(r_ptr, realBrightness);
        if (!success) LibcameraGpuSource.logger.warn("Couldn't set Pi Camera brightness");
    }

    @Override
    public void setGain(int gain) {
        lastGain = gain;

        // Map and clamp gain to values between 1 and 10 (libcamera min and gain that just seems higher
        // than ever needed) from 0 to 100 (UI values).
        var success =
                LibCameraJNI.setAnalogGain(
                        r_ptr, MathUtil.clamp(MathUtils.map(gain, 0.0, 100.0, 1.0, 10.0), 1.0, 10.0));
        if (!success) LibcameraGpuSource.logger.warn("Couldn't set Pi Camera gain");
    }

    @Override
    public void setRedGain(int red) {
        if (sensorModel != LibCameraJNI.SensorModel.OV9281) {
            lastAwbGains = Pair.of(red, lastAwbGains.getSecond());
            setAwbGain(lastAwbGains.getFirst(), lastAwbGains.getSecond());
        }
    }

    @Override
    public void setBlueGain(int blue) {
        if (sensorModel != LibCameraJNI.SensorModel.OV9281) {
            lastAwbGains = Pair.of(lastAwbGains.getFirst(), blue);
            setAwbGain(lastAwbGains.getFirst(), lastAwbGains.getSecond());
        }
    }

    public void setAwbGain(int red, int blue) {
        if (sensorModel != LibCameraJNI.SensorModel.OV9281) {
            var success = LibCameraJNI.setAwbGain(r_ptr, red / 10.0, blue / 10.0);
            if (!success) LibcameraGpuSource.logger.warn("Couldn't set Pi Camera AWB gains");
        }
    }

    @Override
    public FPSRatedVideoMode getCurrentVideoMode() {
        return currentVideoMode;
    }

    @Override
    protected void setVideoModeInternal(VideoMode videoMode) {
        var mode = (FPSRatedVideoMode) videoMode;

        // We need to make sure that other threads don't try to do anything funny while we're recreating
        // the camera
        synchronized (CAMERA_LOCK) {
            if (r_ptr != 0) {
                logger.debug("Stopping libcamera");
                if (!LibCameraJNI.stopCamera(r_ptr)) {
                    logger.error("Couldn't stop a zero copy Pi Camera while switching video modes");
                }
                logger.debug("Destroying libcamera");
                if (!LibCameraJNI.destroyCamera(r_ptr)) {
                    logger.error("Couldn't destroy a zero copy Pi Camera while switching video modes");
                }
            }

            logger.debug("Creating libcamera");
            r_ptr =
                    LibCameraJNI.createCamera(
                            getConfiguration().matchedCameraInfo.path(),
                            mode.width,
                            mode.height,
                            (m_rotationMode == ImageRotationMode.DEG_180_CCW ? 180 : 0));
            if (r_ptr == 0) {
                logger.error("Couldn't create a zero copy Pi Camera while switching video modes");
                if (!LibCameraJNI.destroyCamera(r_ptr)) {
                    logger.error("Couldn't destroy a zero copy Pi Camera while switching video modes");
                }
            }
            logger.debug("Starting libcamera");
            if (!LibCameraJNI.startCamera(r_ptr)) {
                logger.error("Couldn't start a zero copy Pi Camera while switching video modes");
            }
        }

        // We don't store last settings on the native side, and when you change video mode these get
        // reset on the native driver's end. Reset em back to be correct
        setExposureRaw(lastManualExposure);
        setAutoExposure(lastAutoExposureActive);
        setBrightness(lastBrightness);
        setGain(lastGain);
        setAwbGain(lastAwbGains.getFirst(), lastAwbGains.getSecond());

        LibCameraJNI.setFramesToCopy(r_ptr, true, true);

        currentVideoMode = mode;
    }

    @Override
    public HashMap<Integer, VideoMode> getAllVideoModes() {
        return videoModes;
    }

    public LibCameraJNI.SensorModel getModel() {
        return sensorModel;
    }

    @Override
    public double getMinExposureRaw() {
        return this.minExposure;
    }

    @Override
    public double getMaxExposureRaw() {
        return this.maxExposure;
    }

    @Override
    public void setAutoWhiteBalance(boolean autowb) {
        throw new RuntimeException("Libcamera doesn't support AWB temp (yet)");
    }

    @Override
    public void setWhiteBalanceTemp(double temp) {
        throw new RuntimeException("Libcamera doesn't support AWB temp -- use red/blue gain instead");
    }

    @Override
    public double getMaxWhiteBalanceTemp() {
        return 2;
    }

    @Override
    public double getMinWhiteBalanceTemp() {
        return 1;
    }
}
