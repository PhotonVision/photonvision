package org.photonvision.vision.frame.provider;

import edu.wpi.first.util.PixelFormat;
import edu.wpi.first.util.RawFrame;
import org.opencv.core.Mat;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.vision.camera.baslerCameras.BaslerCameraSettables;
import org.photonvision.vision.opencv.CVMat;
import org.teamdeadbolts.basler.BaslerJNI;

public class BaslerFrameProvider extends CpuImageProcessor {
    private final BaslerCameraSettables settables;

    static final Logger logger = new Logger(BaslerFrameProvider.class, LogGroup.Camera);

    public BaslerFrameProvider(BaslerCameraSettables settables) {
        this.settables = settables;

        var vidMode = settables.getCurrentVideoMode();
        settables.setVideoMode(vidMode);
        this.cameraPropertiesCached = true;

        BaslerJNI.startCamera(settables.ptr);
    }

    @Override
    public String getName() {
        return "BaslerCameraFrameProvider";
    }

    @Override
    public void release() {
        BaslerJNI.stopCamera(settables.ptr);
        BaslerJNI.destroyCamera(settables.ptr);
        BaslerJNI.cleanUp();
    }

    @Override
    public boolean isConnected() {
        return true; // TODO: implement
    }

    @Override
    public boolean checkCameraConnected() {
        return true; // TODO: implement
    }

    @Override
    CapturedFrame getInputMat() {

        var cameraMode = settables.getCurrentVideoMode();
        var frame = new RawFrame();
        frame.setInfo(cameraMode.width, cameraMode.height, cameraMode.width * 3, PixelFormat.kBGR);

        CVMat ret;
        var start = MathUtils.wpiNanoTime();
        BaslerJNI.awaitNewFrame(settables.ptr);
        Mat mat = new Mat(BaslerJNI.takeFrame(settables.ptr));
        ret = new CVMat(mat, frame);

        return new CapturedFrame(
                ret, settables.getFrameStaticProperties(), start); // TODO: Timestamping is kinda off rn
    }
}
