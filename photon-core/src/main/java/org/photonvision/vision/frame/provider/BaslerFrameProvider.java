package org.photonvision.vision.frame.provider;

import edu.wpi.first.util.PixelFormat;
import edu.wpi.first.util.RawFrame;
import org.opencv.core.Mat;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.vision.camera.baslerCameras.BaslerCameraSource.BaslerVideoMode;
import org.photonvision.vision.camera.baslerCameras.GenericBaslerCameraSettables;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipe.impl.PixelBinPipe;
import org.photonvision.vision.pipe.impl.PixelBinPipe.PixelBinParams;
import org.photonvision.vision.pipe.impl.PixelBinPipe.PixelBinParams.BinMode;
import org.teamdeadbolts.basler.BaslerJNI;

public class BaslerFrameProvider extends CpuImageProcessor {
    private final GenericBaslerCameraSettables settables;

    static final Logger logger = new Logger(BaslerFrameProvider.class, LogGroup.Camera);

    private PixelBinPipe pixelBinPipe = new PixelBinPipe();

    private Runnable connectedCallback;

    public BaslerFrameProvider(GenericBaslerCameraSettables settables, Runnable connectedCallback) {
        this.settables = settables;
        this.connectedCallback = connectedCallback;

        // var vidMode = settables.getCurrentVideoMode();
        // settables.setVideoMode(vidMode);

        BaslerJNI.startCamera(settables.ptr);
    }

    @Override
    public String getName() {
        return "BaslerCameraFrameProvider-" + this.settables.serial;
    }

    @Override
    public void release() {
        BaslerJNI.stopCamera(settables.ptr);
        BaslerJNI.destroyCamera(settables.ptr);
        BaslerJNI.cleanUp();
    }

    @Override
    public boolean isConnected() {
        var serials = BaslerJNI.getConnectedCameras();
        for (String serial : serials) {
            if (serial.equals(settables.serial)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean checkCameraConnected() {
        boolean connected = isConnected();
        if (connected && !cameraPropertiesCached) {
            logger.info("Camera connected! running callback");
            onCameraConnected();
        }

        return connected;
    }

    @Override
    CapturedFrame getInputMat() {
        if (!cameraPropertiesCached && isConnected()) {
            onCameraConnected();
        }
        var cameraMode = settables.getCurrentVideoMode();
        var frame = new RawFrame();
        frame.setInfo(cameraMode.width, cameraMode.height, cameraMode.width * 3, PixelFormat.kBGR);

        CVMat ret;
        var start = MathUtils.wpiNanoTime();
        BaslerJNI.awaitNewFrame(settables.ptr);
        Mat mat = new Mat(BaslerJNI.takeFrame(settables.ptr));
        BaslerVideoMode.BinningConfig binningConfig =
                this.settables.getCurrentVideoMode().binningConfig;
        if (binningConfig.mode != BinMode.NONE) {
            pixelBinPipe.setParams(
                    new PixelBinParams(binningConfig.mode, binningConfig.horz, binningConfig.vert));
            pixelBinPipe.run(mat);
        }

        ret = new CVMat(mat, frame);

        return new CapturedFrame(
                ret, settables.getFrameStaticProperties(), start); // TODO: Timestamping is kinda off rn
    }

    @Override
    public void onCameraConnected() {
        super.onCameraConnected();
        this.connectedCallback.run();
    }
}
