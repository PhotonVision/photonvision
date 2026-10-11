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
    private boolean wantsGreyscaleOutput = false;

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

    private PixelFormat requestedPixelFormat(boolean decodeMjpeg) {
        if (!wantsGreyscaleOutput) {
            return PixelFormat.BGR;
        }

        // Cscore will decode MJPEG to BGR, but we want grayscale, so we have to decode it ourselves
        if (decodeMjpeg) {
            return PixelFormat.UNKNOWN;
        } else {
            return PixelFormat.GRAY;
        }
    }

    @Override
    public CapturedFrame getInputMat() {
        if (!cameraPropertiesCached && camera.isConnected()) {
            onCameraConnected();
        }

        var cameraMode = settables.getCurrentVideoMode();
        boolean decodeMjpeg = wantsGreyscaleOutput && cameraMode.pixelFormat == PixelFormat.MJPEG;

        // Declare these outside so that finally() can deal with releasing anything left over.
        // Null means "not allocated yet" or "ownership moved to CVMat".
        RawFrame frame = new RawFrame();
        Mat wrapped = null; // header only, points into frame
        Mat gray = null; // owns its own decoded data

        try {
            // MJPEG: cscore mutates stride to zero
            // GRAY: cscore sets stride to 1 byte/pixel
            // BGR: cscore sets stride to 3 byte/pixel
            // We leave stride as zero, let cscore tell us
            frame.setInfo(cameraMode.width, cameraMode.height, 0, requestedPixelFormat(decodeMjpeg));

            // if m_blockForFrames :
            // - start waiting for the cvSink to get a new image delivered
            // - Call GrabSinkFrameTimeoutLastTime lastTime = 0
            // otherwise:
            // - get the next frame with a capture timestamp larger than lastTime
            // - Call GrabSinkFrameTimeoutLastTime lastTime = lastTime
            // This is from wpi::nt::Now, or WPIUtilJNI.now(). The epoch from grabFrame is whatever epoch
            // std::steady_clock is      long captureTimeNs =
            long captureTimeNs =
                    CscoreExtras.grabRawSinkFrameTimeoutLastTime(
                            cvSink.getHandle(),
                            frame,
                            frame.getNativeObj(),
                            CSCORE_DEFAULT_FRAME_TIMEOUT,
                            m_blockForFrames ? 0 : lastTime);

            // 0 means capture error
            if (captureTimeNs == 0) {
                logger.error("Error grabbing image: " + cvSink.getError());
                return new CapturedFrame(new CVMat(), settables.getFrameStaticProperties(), 0);
            }

            lastTime = captureTimeNs;

            wrapped = new Mat(CscoreExtras.wrapRawFrame(frame.getNativeObj()));

            if (decodeMjpeg) {
                // Convert wrapped+frame from MJPEG to grayscale. Gray is a new mat with its own
                // heap-allocated buffer
                gray = mjpegToGray(wrapped, frame, cameraMode);
                var result = new CVMat(gray);
                gray = null; // ownership moved to result
                return new CapturedFrame(result, settables.getFrameStaticProperties(), captureTimeNs);
                // finally() will release wrapped and frame for us
            }

            // CVMat will take ownership of the frame + mat, so we don't release it in finally()
            var result = new CVMat(wrapped, frame);
            wrapped = null;
            frame = null;
            return new CapturedFrame(result, settables.getFrameStaticProperties(), captureTimeNs);
        } finally {
            // Release in reverse order
            // Note release does not free header data: see https://github.com/opencv/opencv/pull/29627
            if (gray != null) gray.release();
            if (wrapped != null) wrapped.release();
            if (frame != null) frame.close();
        }
    }

    /**
     * Decodes an MJPEG frame to a grayscale Mat that owns its data (caller must release it). Inputs
     * are not released. Returns an empty Mat on any failure.
     */
    private Mat mjpegToGray(Mat mat, RawFrame frame, VideoMode cameraMode) {
        if (frame.getPixelFormat() != PixelFormat.MJPEG || mat.empty()) {
            logger.error("mjpegToGray got a non-MJPEG or empty input");
            return new Mat();
        }

        Mat gray;
        try {
            gray = Imgcodecs.imdecode(mat, Imgcodecs.IMREAD_GRAYSCALE);
        } catch (CvException e) {
            logger.error("Could not decode MJPEG frame", e);
            return new Mat();
        }

        if (!gray.empty() && (gray.cols() != cameraMode.width || gray.rows() != cameraMode.height)) {
            logger.error("MJPEG frame size does not match camera mode");
            gray.release();
            return new Mat();
        }
        return gray;
    }

    @Override
    public void requestGrayscaleInput(boolean grayscaleInput) {
        this.wantsGreyscaleOutput = grayscaleInput;
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
