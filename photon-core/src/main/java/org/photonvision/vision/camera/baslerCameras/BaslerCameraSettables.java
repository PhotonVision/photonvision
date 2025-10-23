package org.photonvision.vision.camera.baslerCameras;

import edu.wpi.first.cscore.VideoMode;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.util.PixelFormat;
import java.util.HashMap;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.vision.camera.PVCameraInfo.PVBaslerCameraInfo;
import org.photonvision.vision.processes.VisionSourceSettables;
import org.teamdeadbolts.basler.BaslerJNI;

public class BaslerCameraSettables extends VisionSourceSettables {
    public long ptr = 0;
    private VideoMode currentVideoMode;
    private String serial;

    private double[] currentRatios = new double[3];

    public final Object LOCK = new Object();

    protected BaslerCameraSettables(CameraConfiguration configuration) {
        super(configuration);
        if (!(configuration.matchedCameraInfo instanceof PVBaslerCameraInfo)) {
            throw new IllegalArgumentException(
                    "Cannot create Basler Camera Settables from non basler info");
        }

        PVBaslerCameraInfo info = (PVBaslerCameraInfo) configuration.matchedCameraInfo;

        this.serial = configuration.getDevicePath();

        switch (info.getModel()) {
            case daA1280_54uc:
                videoModes.put(0, new VideoMode(PixelFormat.kBGR.getValue(), 1280, 960, 43));
                videoModes.put(1, new VideoMode(PixelFormat.kUYVY.getValue(), 1280, 960, 52));
                break;
            default:
                logger.warn("Unsupported camera model: " + info.getModel().getFriendlyName());
                videoModes.put(0, new VideoMode(PixelFormat.kBGR.getValue(), 1280, 960, 43)); // Just guess
        }

        this.currentVideoMode = videoModes.get(0);
        this.currentRatios = new double[] {1, 1, 1};
    }

    @Override
    public void setExposureRaw(double exposureRaw) {
        logger.debug("Setting exposure to " + exposureRaw);
        boolean success = BaslerJNI.setExposure(ptr, exposureRaw * 1000);
        if (!success) {
            BaslerCameraSource.logger.warn("Failed to set exposure to " + exposureRaw);
        }
    }

    @Override
    public void setAutoExposure(boolean cameraAutoExposure) {
        logger.debug("Setting auto exposure to " + cameraAutoExposure);

        boolean success = BaslerJNI.setAutoExposure(ptr, cameraAutoExposure);
        if (!success) {
            BaslerCameraSource.logger.warn("Failed to set auto exposure to " + cameraAutoExposure);
        }
    }

    @Override
    public void setWhiteBalanceTemp(double temp) {
        throw new RuntimeException("Dont do this, use ratios insted");
    }

    @Override
    public void setAutoWhiteBalance(boolean autowb) {
        logger.debug("Setting auto white balance to " + autowb);
        boolean success = BaslerJNI.setAutoWhiteBalance(ptr, autowb);
        if (!success) {
            BaslerCameraSource.logger.warn("Failed to set auto white balance to " + autowb);
        }
    }

    @Override
    public void setBrightness(int brightness) {
        // TODO
        // BaslerJNI.getMin
    }

    @Override
    public void setGain(int gain) {
        logger.debug("Setting gain to " + gain);
        double min = BaslerJNI.getMinGain(ptr) + 1.0; // No divide by 0
        double max = BaslerJNI.getMaxGain(ptr);
        boolean success =
                BaslerJNI.setGain(ptr, MathUtil.clamp(MathUtils.map(gain, 0.0, 100.0, min, max), min, max));
        if (!success) {
            BaslerCameraSource.logger.warn("Failed to set gain to " + gain);
        }
    }

    @Override
    public void setRedGain(int red) {
        this.currentRatios[0] = MathUtil.clamp(MathUtils.map(red, 0.0, 100, 1.0, 3.0), 1.0, 3.0);
        BaslerJNI.setWhiteBalance(ptr, this.currentRatios);
    }

    @Override
    public void setBlueGain(int blue) {
        this.currentRatios[2] = MathUtil.clamp(MathUtils.map(blue, 0.0, 100, 1.0, 3.0), 1.0, 3.0);
        BaslerJNI.setWhiteBalance(ptr, this.currentRatios);
    }

    @Override
    public VideoMode getCurrentVideoMode() {
        return currentVideoMode;
    }

    @Override
    protected void setVideoModeInternal(VideoMode videoMode) {
        synchronized (LOCK) {
            if (ptr != 0) {
                logger.debug("Stopping camera");
                if (!BaslerJNI.stopCamera(ptr)) {
                    logger.warn("Failed to stop camera when changing video mode");
                }

                logger.debug("Destroying camera");
                if (!BaslerJNI.destroyCamera(ptr)) {
                    logger.warn("Failed to destroy camera when changing video mode");
                }
            }

            logger.debug("Creating camera");

            ptr = BaslerJNI.createCamera(serial);
            if (!BaslerJNI.setPixelFormat(ptr, videoMode.pixelFormat.getValue())) {
                logger.warn("Failed to set pixel format");
                return;
            }

            if (ptr == 0) {
                logger.error("Failed to create camera when changing video mode");
                return;
            }

            logger.debug("Starting camera");
            if (!BaslerJNI.startCamera(ptr)) {
                logger.error("Failed to start camera when changing video mode");
                BaslerJNI.destroyCamera(ptr);
                ptr = 0;
                return;
            }
        }

        this.currentVideoMode = videoMode;
    }

    @Override
    public HashMap<Integer, VideoMode> getAllVideoModes() {
        return videoModes;
    }

    @Override
    public double getMinWhiteBalanceTemp() {
        return 1;
    }

    @Override
    public double getMaxWhiteBalanceTemp() {
        return 2;
    }

    @Override
    public double getMinExposureRaw() {
        // return BaslerJNI.
        return BaslerJNI.getMinExposure(ptr) / 1000;
    }

    @Override
    public double getMaxExposureRaw() {
        return BaslerJNI.getMaxExposure(ptr) / 1000;
    }
}
