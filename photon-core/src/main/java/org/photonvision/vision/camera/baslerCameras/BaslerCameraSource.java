package org.photonvision.vision.camera.baslerCameras;

import edu.wpi.first.cscore.VideoMode;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.camera.CameraQuirk;
import org.photonvision.vision.camera.CameraType;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.provider.BaslerFrameProvider;
import org.photonvision.vision.pipe.impl.PixelBinPipe;
import org.photonvision.vision.processes.VisionSource;
import org.photonvision.vision.processes.VisionSourceSettables;

public class BaslerCameraSource extends VisionSource {
    static final Logger logger = new Logger(BaslerCameraSource.class, LogGroup.Camera);

    private final GenericBaslerCameraSettables settables;
    private BaslerFrameProvider frameProvider;

    private void onCameraConnected() {
        settables.onCameraConnected();
    }

    public BaslerCameraSource(CameraConfiguration cameraConfiguration) {
        super(cameraConfiguration);
        if (cameraConfiguration.matchedCameraInfo.type() != CameraType.BaslerCamera) {
            throw new IllegalArgumentException(
                    "BaslerCameraSource only accepts CameraConfigurations with type BaslerCamera");
        }

        this.getCameraConfiguration().cameraQuirks =
                QuirkyCamera.getQuirkyCamera(-1, -1, cameraConfiguration.matchedCameraInfo.name());

        this.settables = createSettables(cameraConfiguration);
        // this.settables.setupVideoModes();
        this.frameProvider = new BaslerFrameProvider(this.settables, this::onCameraConnected);
        // logger.debug(QuirkyCamera.getQuirkyCamera(-1, -1,
        // cameraConfiguration.matchedCameraInfo.name()).toString());
        // this.getCameraConfiguration().cameraQuirks.quirks.put(CameraQuirk.Gain, true);
        // this.getCameraConfiguration().cameraQuirks.quirks.put(CameraQuirk.AwbRedBlueGain, true); //
        // Not really correct
    }

    protected GenericBaslerCameraSettables createSettables(CameraConfiguration config) {
        var quirks = getCameraConfiguration().cameraQuirks;

        GenericBaslerCameraSettables settables;

        if (quirks.hasQuirk(CameraQuirk.BaslerDaA1280Controls)) {
            logger.info("Using Basler DaA1280 Settables");
            settables = new BaslerDaA1280CameraSettables(config);
        } else {
            logger.debug("Using generic basler settables");
            settables = new GenericBaslerCameraSettables(config);
        }

        return settables;
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

    public static class BaslerVideoMode extends VideoMode {
        public static class BinningConfig {
            public final PixelBinPipe.PixelBinParams.BinMode mode;
            public final int horz;
            public final int vert;

            public BinningConfig(PixelBinPipe.PixelBinParams.BinMode mode, int horz, int vert) {
                this.mode = mode;
                this.horz = horz;
                this.vert = vert;
            }

            @Override
            public String toString() {
                return "BinningConfig[mode="
                        + this.mode
                        + ",horz="
                        + this.horz
                        + ",vert="
                        + this.vert
                        + "]";
            }
        }

        public final BinningConfig binningConfig;

        public BaslerVideoMode(
                int pixelFormat, int width, int height, int fps, BinningConfig binningConfig) {
            super(pixelFormat, width, height, fps);
            this.binningConfig = binningConfig;
        }
    }
}
