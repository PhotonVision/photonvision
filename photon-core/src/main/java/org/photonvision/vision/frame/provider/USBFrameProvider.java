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
import org.wpilib.vision.stream.CameraServer;

public class USBFrameProvider extends CpuImageProcessor {
    private final Logger logger;

    private UsbCamera camera = null;
    private CvSink cvSink = null;

    @SuppressWarnings("SpellCheckingInspection")
    private VisionSourceSettables settables;

    private Runnable connectedCallback;

    private long lastTime = 0;
    private volatile boolean grayscaleInput = false;

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
            return getRawInputMat();
        }
    }

    private CapturedFrame getRawInputMat() {
        var cameraMode = settables.getCurrentVideoMode();
        boolean captureGrayscale = grayscaleInput;
        // CSCore's MJPEG-to-gray conversion first decodes BGR. Capture compressed bytes instead
        // so OpenCV can decode JPEG luminance directly at the original camera resolution.
        boolean decodeMjpeg = captureGrayscale && cameraMode.pixelFormat == PixelFormat.MJPEG;
        // UNKNOWN asks for the original image. CSCore's converted-image API rejects MJPEG.
        var format =
                decodeMjpeg ? PixelFormat.UNKNOWN : captureGrayscale ? PixelFormat.GRAY : PixelFormat.BGR;
        var frame = new RawFrame();
        CVMat image = null;
        boolean ownershipTransferred = false;
        long captureTimeNs = 0;
        try {
            frame.setInfo(
                    cameraMode.width,
                    cameraMode.height,
                    decodeMjpeg ? 0 : cameraMode.width * (captureGrayscale ? 1 : 3),
                    format);
            // Zero waits for a newly arriving frame, matching CvSink.grabFrame's blocking mode.
            // Otherwise allow the latest frame as long as it differs from our previous capture.
            captureTimeNs =
                    CscoreExtras.grabRawSinkFrameTimeoutLastTime(
                            cvSink.getHandle(),
                            frame.getNativeObj(),
                            CSCORE_DEFAULT_FRAME_TIMEOUT,
                            m_blockForFrames ? 0 : lastTime);
            lastTime = captureTimeNs;
            if (captureTimeNs == 0) {
                logger.error("Error grabbing image: " + cvSink.getError());
                return new CapturedFrame(new CVMat(), settables.getFrameStaticProperties(), 0);
            }

            if (decodeMjpeg && CscoreExtras.getPixelFormat(frame) != PixelFormat.MJPEG) {
                // A camera mode change can leave one frame from the previous mode in the sink.
                return new CapturedFrame(new CVMat(), settables.getFrameStaticProperties(), 0);
            }
            image = new CVMat(new Mat(CscoreExtras.wrapRawFrame(frame.getNativeObj())), frame);
            ownershipTransferred = true;
            if (decodeMjpeg) {
                image = decodeMjpegGrayscale(image);
                if (image.getMat().empty()) {
                    logger.error("Error decoding MJPEG image");
                }
            }
            return new CapturedFrame(image, settables.getFrameStaticProperties(), captureTimeNs);
        } catch (RuntimeException ex) {
            if (image != null) image.release();
            logger.error("Error grabbing image", ex);
            return new CapturedFrame(new CVMat(), settables.getFrameStaticProperties(), captureTimeNs);
        } finally {
            if (!ownershipTransferred) {
                frame.close();
            }
        }
    }

    /** Decode into owned storage before releasing the compressed frame and its native buffer. */
    static CVMat decodeMjpegGrayscale(CVMat encoded) {
        try {
            if (encoded.getMat().empty()) return new CVMat();
            return new CVMat(Imgcodecs.imdecode(encoded.getMat(), Imgcodecs.IMREAD_GRAYSCALE));
        } finally {
            encoded.release();
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
