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
    private volatile boolean grayscaleInput = false;

    /** What to ask CSCore for, given whether the pipeline wants grayscale and the camera's format. */
    enum CapturePlan {
        /** Color input, for pipelines that use color. */
        BGR(PixelFormat.BGR),
        /**
         * Luminance only. CSCore takes it straight from YUYV/UYVY and passes GRAY through. It returns
         * BGR sources unconverted, and the GREYSCALE processing step converts those.
         */
        GRAY(PixelFormat.GRAY),
        /**
         * The original compressed image (UNKNOWN asks for it), decoded straight to luminance here.
         * CSCore always decodes MJPEG to full color first, even when GRAY is requested.
         */
        MJPEG_TO_GRAY(PixelFormat.UNKNOWN);

        final PixelFormat requestFormat;

        CapturePlan(PixelFormat requestFormat) {
            this.requestFormat = requestFormat;
        }

        static CapturePlan choose(boolean grayscaleInput, PixelFormat cameraFormat) {
            if (!grayscaleInput) return BGR;
            return cameraFormat == PixelFormat.MJPEG ? MJPEG_TO_GRAY : GRAY;
        }
    }

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

        // Read the mode and its properties together so the returned image agrees with both
        var cameraMode = settables.getCurrentVideoMode();
        var staticProps = settables.getFrameStaticProperties();
        var plan = CapturePlan.choose(grayscaleInput, cameraMode.pixelFormat);

        // CSCore scales converted images to the requested size and fills in the stride itself
        // TODO - consider a frame pool
        var frame = new RawFrame();
        frame.setInfo(cameraMode.width, cameraMode.height, 0, plan.requestFormat);

        long captureTimeNs;
        try {
            // This is from wpi::nt::Now, or WPIUtilJNI.now(). The epoch from grabFrame is nS since
            // Hal::initialize was called. A last frame time of zero waits for a new frame, exactly as
            // CvSink.grabFrame does; otherwise any frame newer than our previous capture is returned.
            captureTimeNs =
                    CscoreExtras.grabRawSinkFrameTimeoutLastTime(
                            cvSink.getHandle(),
                            frame.getNativeObj(),
                            CSCORE_DEFAULT_FRAME_TIMEOUT,
                            m_blockForFrames ? 0 : lastTime);
        } catch (RuntimeException e) {
            frame.close();
            logger.error("Error grabbing image", e);
            return new CapturedFrame(new CVMat(), staticProps, 0);
        }
        lastTime = captureTimeNs;

        if (captureTimeNs == 0) {
            frame.close();
            logger.error("Error grabbing image: " + cvSink.getError());
            return new CapturedFrame(new CVMat(), staticProps, 0);
        }

        // The Mat wraps the RawFrame's buffer, so the CVMat takes ownership of the frame
        var capturedFormat = CscoreExtras.getPixelFormat(frame);
        var image = new CVMat(new Mat(CscoreExtras.wrapRawFrame(frame.getNativeObj())), frame);
        if (plan == CapturePlan.MJPEG_TO_GRAY) {
            image = decodeMjpegGrayscale(image, capturedFormat, cameraMode.width, cameraMode.height);
            if (image.getMat().empty()) {
                logger.warn("Dropped a " + capturedFormat + " frame that could not be decoded as MJPEG");
            }
        }
        return new CapturedFrame(image, staticProps, captureTimeNs);
    }

    /**
     * Decode a compressed capture straight to luminance, scaled to the camera mode the same way
     * CSCore scales converted images. Always releases the capture. Returns an empty image when the
     * capture is not MJPEG, such as one frame left in the sink from the previous mode.
     */
    static CVMat decodeMjpegGrayscale(
            CVMat captured, PixelFormat capturedFormat, int width, int height) {
        try {
            if (capturedFormat != PixelFormat.MJPEG || captured.getMat().empty()) return new CVMat();

            var gray = Imgcodecs.imdecode(captured.getMat(), Imgcodecs.IMREAD_GRAYSCALE);
            if (!gray.empty() && (gray.cols() != width || gray.rows() != height)) {
                Imgproc.resize(gray, gray, new Size(width, height));
            }
            return new CVMat(gray);
        } finally {
            captured.release();
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
