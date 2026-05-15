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

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import org.opencv.core.Mat;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.configuration.PathManager;
import org.photonvision.common.dataflow.networktables.NetworkTablesManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.PathSafety;
import org.photonvision.jni.CscoreExtras;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipeline.FrameRecorder;
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

        if (m_blockForFrames) {
            // We allocate memory so we don't fill a Mat in use by another thread (memory model is easier)
            var mat = new CVMat();
            // This is from wpi::nt::Now, or WPIUtilJNI.now(). The epoch from grabFrame is uS since
            // Hal::initialize was called
            // TODO - under the hood, this incurs an extra copy. We should avoid this, if we
            // can.
            long captureTimeNs = cvSink.grabFrame(mat.getMat(), CSCORE_DEFAULT_FRAME_TIMEOUT) * 1000;

            if (captureTimeNs == 0) {
                var error = cvSink.getError();
                logger.error("Error grabbing image: " + error);
            }

            offerToRecorder(mat, captureTimeNs);
            return new CapturedFrame(mat, settables.getFrameStaticProperties(), captureTimeNs);
        } else {
            // We allocate memory so we don't fill a Mat in use by another thread (memory model is easier)
            // TODO - consider a frame pool
            // TODO - getCurrentVideoMode is a JNI call for us, but profiling indicates it's fast
            var cameraMode = settables.getCurrentVideoMode();
            var frame = new RawFrame();
            frame.setInfo(
                    cameraMode.width,
                    cameraMode.height,
                    // hard-coded 3 channel
                    cameraMode.width * 3,
                    PixelFormat.kBGR);

            // This is from wpi::nt::Now, or WPIUtilJNI.now(). The epoch from grabFrame is uS since
            // Hal::initialize was called
            long captureTimeUs =
                    CscoreExtras.grabRawSinkFrameTimeoutLastTime(
                            cvSink.getHandle(), frame.getNativeObj(), CSCORE_DEFAULT_FRAME_TIMEOUT, lastTime);
            lastTime = captureTimeUs;

            CVMat ret;

            if (captureTimeUs == 0) {
                var error = cvSink.getError();
                logger.error("Error grabbing image: " + error);

                frame.close();
                ret = new CVMat();
            } else {
                // No error! yay
                var mat = new Mat(CscoreExtras.wrapRawFrame(frame.getNativeObj()));

                ret = new CVMat(mat, frame);
            }

            offerToRecorder(ret, captureTimeUs * 1000);
            return new CapturedFrame(ret, settables.getFrameStaticProperties(), captureTimeUs * 1000);
        }
    }

    // Snapshot the volatile recorder once: the NT listener thread can null it between the read
    // and the use. recordFrame self-guards on its AtomicBooleans, so late submissions during
    // release() drop cleanly.
    private void offerToRecorder(CVMat mat, long captureNs) {
        FrameRecorder rec = frameRecorder;
        if (rec != null && rec.isRecording()) {
            rec.recordFrame(mat, captureNs);
        }
    }

    @Override
    public String getName() {
        return "USBFrameProvider - " + cvSink.getName();
    }

    // Synchronized for symmetry with setRecording: a concurrent setRecording(true) mid-
    // construction would otherwise leave an orphan recorder (created after release saw null).
    @Override
    public synchronized void release() {
        CameraServer.removeServer(cvSink.getName());
        cvSink.close();
        cvSink = null;
        if (frameRecorder != null) {
            frameRecorder.release();
            frameRecorder = null;
        }
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

    // Synchronized to serialize start/stop transitions: without it, two concurrent
    // setRecording(true) calls can both pass the getRecording() check while frameRecorder
    // is null, both construct a FrameRecorder, and the second assignment orphans the first
    // (writer thread + file handles leaked forever, no path to release).
    @Override
    public synchronized void setRecording(boolean shouldRecord) {
        if (shouldRecord) {
            String camPath = settables.getConfiguration().uniqueName;

            String recordingPath = NetworkTablesManager.getInstance().getMatchData();
            if (recordingPath == null || recordingPath.isBlank()) {
                // No match is currently active (or FMS published whitespace), use timestamp.
                recordingPath =
                        DateTimeFormatter.ofPattern(PathManager.LOG_DATE_TIME_FORMAT)
                                .format(java.time.LocalDateTime.now());
            }

            // matchData is FMS-supplied and reaches us via an unauthenticated NT topic; sanitize
            // before letting it become a path segment.
            Path outputPath;
            try {
                outputPath =
                        PathSafety.safeResolve(
                                ConfigManager.getInstance().getRecordingsDirectory().toPath(),
                                camPath,
                                recordingPath);
            } catch (SecurityException e) {
                logger.error("Refusing to start recording at unsafe path: " + e.getMessage());
                return;
            }

            if (frameRecorder != null && frameRecorder.isRecording()) {
                logger.warn("Frame recorder is already recording!");
                return;
            }

            try {
                frameRecorder = new FrameRecorder(outputPath);
                frameRecorder.startRecording();
            } catch (Exception e) {
                logger.error("Exception creating FrameRecorder", e);
                return;
            }
        } else {
            if (frameRecorder != null) {
                frameRecorder.stopRecording();
                frameRecorder.release();
                frameRecorder = null;
            }
        }
    }

    @Override
    public boolean getRecording() {
        FrameRecorder rec = frameRecorder;
        return rec != null && rec.isRecording();
    }
}
