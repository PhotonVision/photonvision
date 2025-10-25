package org.photonvision.vision.camera.baslerCameras;

import edu.wpi.first.util.PixelFormat;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.vision.camera.CameraQuirk;
import org.photonvision.vision.camera.baslerCameras.BaslerCameraSource.BaslerVideoMode;
import org.photonvision.vision.camera.baslerCameras.BaslerCameraSource.BaslerVideoMode.BinningConfig;
import org.photonvision.vision.pipe.impl.PixelBinPipe.PixelBinParams.BinMode;

public class BaslerDaA1280CameraSettables extends GenericBaslerCameraSettables {

    protected BaslerDaA1280CameraSettables(CameraConfiguration configuration) {
        super(configuration);

        this.maxExposure = 1000;
        this.maxGain = 18;

        this.getConfiguration().cameraQuirks.quirks.put(CameraQuirk.Gain, true);
        this.getConfiguration().cameraQuirks.quirks.put(CameraQuirk.AwbRedBlueGain, true);
    }

    @Override
    protected void setupVideoModes() {
        videoModes.put(
                0,
                new BaslerVideoMode(
                        PixelFormat.kBGR.getValue(), 1280, 960, 43, new BinningConfig(BinMode.NONE, 0, 0)));
        videoModes.put(
                1,
                new BaslerVideoMode(
                        PixelFormat.kUYVY.getValue(), 1280, 960, 52, new BinningConfig(BinMode.NONE, 0, 0)));
        videoModes.put(
                2,
                new BaslerVideoMode(
                        PixelFormat.kBGR.getValue(),
                        1280 / 2,
                        960 / 2,
                        43,
                        new BinningConfig(BinMode.SUM, 2, 2)));
        videoModes.put(
                3,
                new BaslerVideoMode(
                        PixelFormat.kBGR.getValue(),
                        1280 / 2,
                        960 / 2,
                        43,
                        new BinningConfig(BinMode.AVERAGE, 2, 2)));
    }
}
