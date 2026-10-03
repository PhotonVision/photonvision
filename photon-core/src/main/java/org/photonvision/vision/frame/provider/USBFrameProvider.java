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

package org.photonvision.vision.frame.provider;

import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.jni.CscoreExtras;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.processes.VisionSourceSettables;
import org.wpilib.util.PixelFormat;
import org.wpilib.util.RawFrame;
import org.wpilib.vision.camera.CvSink;
import org.wpilib.vision.camera.UsbCamera;
import org.wpilib.vision.camera.VideoMode;
import org.wpilib.vision.stream.CameraServer;

public class USBFrameProvider extends CpuImageProcessor {
    private final Logger logger;

    private UsbCamera camera = null;
    private CvSink cvSink = null;

    @SuppressWarnings("SpellCheckingInspection")
    private VisionSourceSettables settables;

    private Runnable connectedCallback;

    private long lastTime = 0;
    private boolean grayscaleInput = false;

    @SuppressWarnings("SpellCheckingInspection")
    public USBFrameProvider(
            UsbCamera camera, VisionSourceSettables visionSettables, Runnable connectedCallback) {
        this.camera = camera;
        this.cvSink = CameraServer.getVideo(this.camera);
        this.logger =
                new Logger(
                        USBFrameProvider.class, visionSettables.getConfiguration().nickname, LogGroup.Camera);
        this.cvSink.setEnabled(true);

        this.settables = visionSettables;

        this.connectedCallback = connectedCallback;
    }

    final double CSCORE_DEFAULT_FRAME_TIMEOUT = 1.0 / 4.0;

    @Override
    public CapturedFrame getInputMat() {
        if (!cameraPropertiesCached && camera.isConnected()) {
            onCameraConnected();
        }

        // We allocate memory so we don't fill a Mat in use by another thread (memory model is easier)
        // TODO - consider a frame pool
        // TODO - getCurrentVideoMode is a JNI call for us, but profiling indicates it's fast
        var cameraMode = settables.getCurrentVideoMode();
        boolean decodeMjpeg = grayscaleInput && cameraMode.pixelFormat == PixelFormat.MJPEG;
        var frame = new RawFrame();
        frame.setInfo(
                cameraMode.width,
                cameraMode.height,
                grayscaleInput ? 0 : cameraMode.width * 3,
                decodeMjpeg ? PixelFormat.UNKNOWN : grayscaleInput ? PixelFormat.GRAY : PixelFormat.BGR);

        // if m_blockForFrames :
        // - start waiting for the cvSink to get a new image delivered
        // - Call GrabSinkFrameTimeoutLastTime lastTime = 0
        // otherwise:
        // - get the next frame with a capture timestamp larger than lastTime
        // - Call GrabSinkFrameTimeoutLastTime lastTime = lastTime
        // This is from wpi::nt::Now, or WPIUtilJNI.now(). The epoch from grabFrame is whatever epoch
        // std::steady_clock is      long captureTimeNs =
        long lastFrameTime = m_blockForFrames ? 0 : lastTime;
        long captureTimeNs =
                CscoreExtras.grabRawSinkFrameTimeoutLastTime(
                        cvSink.getHandle(),
                        frame,
                        frame.getNativeObj(),
                        CSCORE_DEFAULT_FRAME_TIMEOUT,
                        lastFrameTime);
        lastTime = captureTimeNs;

        System.out.println(
                "now "
                        + System.nanoTime()
                        + " rv "
                        + captureTimeNs
                        + " frame time "
                        + frame.getTimestamp()
                        + " src "
                        + frame.getTimestampSource()
                        + " last "
                        + lastFrameTime);

        // 0 means capture error
        if (captureTimeNs == 0) {
            var error = cvSink.getError();
            logger.error("Error grabbing image: " + error);

            frame.close();

            return new CapturedFrame(new CVMat(), settables.getFrameStaticProperties(), captureTimeNs);
        }

        // This mat does not own frame data, releasing it just releases headers. Release frame to
        // release image data. Users must track frame + mat together
        var mat = new Mat(CscoreExtras.wrapRawFrame(frame.getNativeObj()));

        CVMat colorImage = null;

        if (decodeMjpeg) {
            colorImage = mjpegToGray(mat, frame, cameraMode);
        } else {
            // Not MJPEG, no decoding
            colorImage = new CVMat(mat, frame);
        }

        return new CapturedFrame(colorImage, settables.getFrameStaticProperties(), captureTimeNs);
    }

    private CVMat mjpegToGray(Mat mat, RawFrame frame, VideoMode cameraMode) {
        Mat grayMat = null;

        try {
            if (frame.getPixelFormat() == PixelFormat.MJPEG && !mat.empty()) {
                grayMat = Imgcodecs.imdecode(mat, Imgcodecs.IMREAD_GRAYSCALE);
                if (!grayMat.empty()
                        && (grayMat.cols() != cameraMode.width || grayMat.rows() != cameraMode.height)) {
                    // This shoulldn't happen
                    logger.error("Got MJPG frame that does not match size of cameramode. Returning none");
                    grayMat.release();
                    return new CVMat();
                }
            }
        } catch (CvException e) {
            logger.error("Could not decode MJPEG frame", e);

            grayMat.release();
            return new CVMat();
        }

        return new CVMat(grayMat);
    }

    @Override
    public void requestGrayscaleInput(boolean grayscaleInput) {
        this.grayscaleInput = grayscaleInput;
    }

    @Override
    public String getName() {
        return "USBFrameProvider - " + cvSink.getName();
    }

    @Override
    public void release() {
        CameraServer.removeServer(cvSink.getName());
        cvSink.close();
        cvSink = null;
    }

    @Override
    public void onCameraConnected() {
        logger.info("Camera connected! running callback");

        super.onCameraConnected();

        this.connectedCallback.run();
    }

    @Override
    public boolean checkCameraConnected() {
        return camera.isConnected();
    }

    public void updateSettables(VisionSourceSettables settables) {
        this.settables = settables;
    }
}
