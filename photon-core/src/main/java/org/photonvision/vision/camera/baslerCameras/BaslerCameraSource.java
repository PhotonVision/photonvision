package org.photonvision.vision.camera.baslerCameras;

import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.camera.CameraType;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.provider.BaslerFrameProvider;
import org.photonvision.vision.processes.VisionSource;
import org.photonvision.vision.processes.VisionSourceSettables;

public class BaslerCameraSource extends VisionSource {
    static final Logger logger = new Logger(BaslerCameraSource.class, LogGroup.Camera);

    private final BaslerCameraSettables settables;
    private BaslerFrameProvider frameProvider;

    public BaslerCameraSource(CameraConfiguration cameraConfiguration) {
        super(cameraConfiguration);
        if (cameraConfiguration.matchedCameraInfo.type() != CameraType.BaslerCamera) {
            throw new IllegalArgumentException(
                    "BaslerCameraSource only accepts CameraConfigurations with type BaslerCamera");
        }

        this.settables = new BaslerCameraSettables(cameraConfiguration);
        this.frameProvider = new BaslerFrameProvider(this.settables);
        this.getCameraConfiguration().cameraQuirks = QuirkyCamera.DefaultCamera;
    }

    @Override
    public void release() {
        this.frameProvider.release();
        this.frameProvider = null;
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
        // TODO: implement
    }
}
