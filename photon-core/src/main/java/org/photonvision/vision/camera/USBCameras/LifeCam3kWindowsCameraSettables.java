package org.photonvision.vision.camera.USBCameras;

import edu.wpi.first.cscore.UsbCamera;
import edu.wpi.first.cscore.VideoException;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.util.math.MathUtils;

public class LifeCam3kWindowsCameraSettables extends GenericUSBCameraSettables {

    private static int[] allowableExposures = {
        5, 10, 20, 39, 78, 156, 312, 625, 1250, 2500, 5000, 10000, 20000
    };

    public LifeCam3kWindowsCameraSettables(CameraConfiguration configuration, UsbCamera camera) {
        super(configuration, camera);
    }

    @Override
    protected void setUpExposureProperties() {

        autoExposureProp = null; // Not Used

        exposureAbsProp = camera.getProperty("raw_Exposure");
        this.minExposure = exposureAbsProp.getMin();
        this.maxExposure = exposureAbsProp.getMax();
    }

    @Override
    public void setExposureRaw(double exposureRaw) {
        if (exposureRaw >= 0.0) {
            try {

                int propVal = (int) MathUtils.limit(exposureRaw, minExposure, maxExposure);

                propVal = MathUtils.quantize(propVal, allowableExposures);

                logger.debug(
                        "Setting property "
                                + autoExposureProp.getName()
                                + " to "
                                + propVal
                                + " (user requested "
                                + exposureRaw
                                + " μs)");

                exposureAbsProp.set(propVal);

                this.lastExposureRaw = exposureRaw;

                // Lifecam requires setting brightness again after exposure
                // And it requires setting it twice, ensuring the value is different
                // This camera is very bork.
                if (lastBrightness >= 0) {
                    setBrightness(lastBrightness - 1);
                }

            } catch (VideoException e) {
                logger.error("Failed to set camera exposure!", e);
            }
        }
    }

    public void setAutoExposure(boolean cameraAutoExposure) {
        logger.debug("Setting auto exposure to " + cameraAutoExposure);

        if (!cameraAutoExposure) {

            // Most cameras leave exposure time absolute at the last value from their AE
            // algorithm.
            // Set it back to the exposure slider value
            setExposureRaw(this.lastExposureRaw);

        } else {
            softSet("WhiteBalance", 4000);
            exposureAbsProp.set(0);
        }
    }

    @Override
    public void setAllCamDefaults() {
        // Common settings for all cameras to attempt to get their image
        // as close as possible to what we want for image processing
        softSet("raw_Contrast", 5);
        softSet("raw_Saturation", 85);
        softSet("raw_Sharpness", 25);
        softSet("WhiteBalance", 4000);
    }
}
