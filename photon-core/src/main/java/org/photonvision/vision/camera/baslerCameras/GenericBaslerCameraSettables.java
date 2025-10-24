package org.photonvision.vision.camera.baslerCameras;

import edu.wpi.first.cscore.VideoMode;
import edu.wpi.first.math.MathUtil;
import java.util.HashMap;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.vision.camera.PVCameraInfo.PVBaslerCameraInfo;
import org.photonvision.vision.camera.baslerCameras.BaslerCameraSource.BaslerVideoMode;
import org.photonvision.vision.processes.VisionSourceSettables;
import org.teamdeadbolts.basler.BaslerJNI;

public class GenericBaslerCameraSettables extends VisionSourceSettables {
    public long ptr = 0;
    public String serial;

    public final Object LOCK = new Object();

    protected BaslerVideoMode currentVideoMode;

    protected double minExposure = -1;
    protected double maxExposure = 1000;

    protected double minGain = 1;
    protected double maxGain = 1;

    protected double lastExposure = -1;
    protected int lastGain = -1;

    protected PVBaslerCameraInfo info;

    protected GenericBaslerCameraSettables(CameraConfiguration configuration) {
        super(configuration);
        if (!(configuration.matchedCameraInfo instanceof PVBaslerCameraInfo)) {
            throw new IllegalArgumentException(
                    "Cannot create Basler Camera Settables from non basler info");
        }

        this.info = (PVBaslerCameraInfo) configuration.matchedCameraInfo;
        this.serial = configuration.getDevicePath();
    }

    @Override
    public void setExposureRaw(double exposureRaw) {
        this.lastExposure = exposureRaw;
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
        if (!cameraAutoExposure) setExposureRaw(this.lastExposure);
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

        // double min = BaslerJNI.getMinGain(ptr) + 1.0; // No divide by 0
        // double max = BaslerJNI.getMaxGain(ptr);
        this.lastGain = gain;
        boolean success =
                BaslerJNI.setGain(
                        ptr,
                        MathUtil.clamp(MathUtils.map(gain, 0.0, 100.0, minGain, maxGain), minGain, maxGain));
        if (!success) {
            BaslerCameraSource.logger.warn("Failed to set gain to " + gain);
        }
    }

    // @Override
    // public void setRedGain(int red) {
    //     this.currentRatios[0] = MathUtil.clamp(MathUtils.map(red, 0.0, 100, 1.0, 3.0), 1.0, 3.0);
    //     BaslerJNI.setWhiteBalance(ptr, this.currentRatios);
    // }

    // @Override
    // public void setBlueGain(int blue) {
    //     this.currentRatios[2] = MathUtil.clamp(MathUtils.map(blue, 0.0, 100, 1.0, 3.0), 1.0, 3.0);
    //     BaslerJNI.setWhiteBalance(ptr, this.currentRatios);
    // }

    @Override
    public BaslerVideoMode getCurrentVideoMode() {
        return currentVideoMode;
    }

    public void setVideoMode(VideoMode mode) {
        var bMode = (BaslerVideoMode) mode;
        logger.info(
                "Setting video mode to "
                        + "FPS: "
                        + mode.fps
                        + " Width: "
                        + mode.width
                        + " Height: "
                        + mode.height
                        + " Pixel Format: "
                        + mode.pixelFormat
                        + " Binning config: "
                        + bMode.binningConfig);
        setVideoModeInternal(mode);
        calculateFrameStaticProps();
    }

    @Override
    protected void setVideoModeInternal(VideoMode videoMode) {
        var mode = (BaslerVideoMode) videoMode;
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
            if (!BaslerJNI.setPixelFormat(ptr, mode.pixelFormat.getValue())) {
                logger.warn("Failed to set pixel format");
                return;
            }
            if (this.lastGain != -1) {
                this.setGain(this.lastGain);
            }
            if (this.lastExposure != -1) {
                this.setExposureRaw(this.lastExposure);
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

        this.currentVideoMode = mode;
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
        return minExposure;
    }

    @Override
    public double getMaxExposureRaw() {
        return maxExposure;
    }

    @Override
    public void onCameraConnected() {
        super.onCameraConnected();
        setupVideoModes();
        if (!videoModes.isEmpty()) this.currentVideoMode = (BaslerVideoMode) videoModes.get(0);
        else logger.warn("Video modes empty");

        calculateFrameStaticProps();
    }

    protected void setupVideoModes() {}
}
