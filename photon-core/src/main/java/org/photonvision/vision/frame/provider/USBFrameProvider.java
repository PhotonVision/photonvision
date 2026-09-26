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
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.jni.CscoreExtras;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.processes.VisionSourceSettables;
import org.wpilib.util.PixelFormat;
import org.wpilib.util.RawFrame;
import org.wpilib.vision.camera.CvSink;
import org.wpilib.vision.camera.UsbCamera;
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

        if (m_blockForFrames && !grayscaleInput) {
            // We allocate memory so we don't fill a Mat in use by another thread (memory model is easier)
            var mat = new CVMat();
            // This is from wpi::nt::Now, or WPIUtilJNI.now(). The epoch from grabFrame is nS since
            // Hal::initialize was called
            // TODO - under the hood, this incurs an extra copy. We should avoid this, if we
            // can.
            long captureTimeNs = cvSink.grabFrame(mat.getMat(), CSCORE_DEFAULT_FRAME_TIMEOUT);

            if (captureTimeNs == 0) {
                var error = cvSink.getError();
                logger.error("Error grabbing image: " + error);
            }

            return new CapturedFrame(mat, settables.getFrameStaticProperties(), captureTimeNs);
        } else {
            // We allocate memory so we don't fill a Mat in use by another thread (memory model is easier)
            // TODO - consider a frame pool
            // TODO - getCurrentVideoMode is a JNI call for us, but profiling indicates it's fast
            var cameraMode = settables.getCurrentVideoMode();
            boolean decodeMjpeg = grayscaleInput && cameraMode.pixelFormat == PixelFormat.MJPEG;
            var frame = new RawFrame();
            frame.setInfo(
                    cameraMode.width,
                    cameraMode.height,
                    0,
                    // UNKNOWN preserves MJPEG bytes so we can decode directly to luminance.
                    decodeMjpeg ? PixelFormat.UNKNOWN : grayscaleInput ? PixelFormat.GRAY : PixelFormat.BGR);

            // This is from wpi::nt::Now, or WPIUtilJNI.now(). The epoch from grabFrame is nS since
            // Hal::initialize was called
            long captureTimeNs;
            try {
                captureTimeNs =
                        CscoreExtras.grabRawSinkFrameTimeoutLastTime(
                                cvSink.getHandle(),
                                frame.getNativeObj(),
                                CSCORE_DEFAULT_FRAME_TIMEOUT,
                                m_blockForFrames ? 0 : lastTime);
            } catch (RuntimeException e) {
                frame.close();
                logger.error("Error grabbing image", e);
                return new CapturedFrame(new CVMat(), settables.getFrameStaticProperties(), 0);
            }
            lastTime = captureTimeNs;

            CVMat ret;

            if (captureTimeNs == 0) {
                var error = cvSink.getError();
                logger.error("Error grabbing image: " + error);

                frame.close();
                ret = new CVMat();
            } else {
                // No error! yay
                var mat = new Mat(CscoreExtras.wrapRawFrame(frame.getNativeObj()));

                ret = new CVMat(mat, frame);
                if (decodeMjpeg) {
                    Mat gray = null;
                    try {
                        // A frame from the previous camera mode may still be in the sink.
                        if (PixelFormat.getFromInt(CscoreExtras.getPixelFormatNative(frame.getNativeObj()))
                                        == PixelFormat.MJPEG
                                && !mat.empty()) {
                            gray = Imgcodecs.imdecode(mat, Imgcodecs.IMREAD_GRAYSCALE);
                            if (!gray.empty()
                                    && (gray.cols() != cameraMode.width || gray.rows() != cameraMode.height)) {
                                Imgproc.resize(gray, gray, new Size(cameraMode.width, cameraMode.height));
                            }
                        }
                    } catch (CvException e) {
                        if (gray != null) gray.release();
                        gray = null;
                        logger.error("Could not decode MJPEG frame", e);
                    } finally {
                        ret.release();
                    }
                    ret = gray == null ? new CVMat() : new CVMat(gray);
                }
            }

            return new CapturedFrame(ret, settables.getFrameStaticProperties(), captureTimeNs);
        }
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
