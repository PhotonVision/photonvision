package org.photonvision.vision.camera.baslerCameras;

import edu.wpi.first.cscore.VideoMode;
import edu.wpi.first.util.PixelFormat;
import java.util.HashMap;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.vision.camera.PVCameraInfo.PVBaslerCameraInfo;
import org.photonvision.vision.processes.VisionSourceSettables;
import org.teamdeadbolts.basler.BaslerJNI;

public class BaslerCameraSettables extends VisionSourceSettables {
    public long ptr = 0;
    private VideoMode currentVideoMode;
    private String serial;

    public final Object LOCK = new Object();

    protected BaslerCameraSettables(CameraConfiguration configuration) {
        // configuration.matchedCameraInfo.path()
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
            default:
                break;
        }

        this.currentVideoMode = videoModes.get(0);
    }

    @Override
    public void setExposureRaw(double exposureRaw) {
        logger.debug("Setting exposure to " + exposureRaw);
        boolean success = BaslerJNI.setExposure(ptr, exposureRaw);
        if (!success) {
            BaslerCameraSource.logger.warn("Failed to set exposure to " + exposureRaw);
        }
    }

    @Override
    public void setAutoExposure(boolean cameraAutoExposure) {
        logger.debug("Setting auto exposure to " + cameraAutoExposure);
        logger.debug("PTR: " + ptr);
        logger.debug("Supported formats: ");
        for (int f : BaslerJNI.getSupportedPixelFormats(ptr)) {
          logger.debug(PixelFormat.getFromInt(f).toString());
        }

        boolean success = BaslerJNI.setAutoExposure(ptr, cameraAutoExposure);
        if (!success) {
            BaslerCameraSource.logger.warn("Failed to set auto exposure to " + cameraAutoExposure);
        }
    }

    @Override
    public void setWhiteBalanceTemp(double temp) {
        logger.debug("Setting white balance to " + temp);
        boolean success = BaslerJNI.setWhiteBalance(ptr, new double[] {temp, temp, temp});
        if (!success) {
            BaslerCameraSource.logger.warn("Failed to set white balance to " + temp);
        }
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
        boolean success = BaslerJNI.setGain(ptr, gain);
        if (!success) {
            BaslerCameraSource.logger.warn("Failed to set gain to " + gain);
        }
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
        return BaslerJNI.getMinWhiteBalance(ptr);
    }

    @Override
    public double getMaxWhiteBalanceTemp() {
        return BaslerJNI.getMaxWhiteBalance(ptr);
    }

     @Override
    public double getMinExposureRaw() {
        // return BaslerJNI.
        return BaslerJNI.getMinExposure(ptr);
    }

    @Override
    public double getMaxExposureRaw() {
        return BaslerJNI.getMaxExposure(ptr);
    }

}
