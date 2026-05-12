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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.opencv.CVMat;

/**
 * Replay-side counterpart to {@link org.photonvision.vision.pipeline.FrameRecorder}. Reads a
 * recording directory ({@code frames/<seq>.jpg} + {@code metadata.jsonl}) and emits
 * {@link CapturedFrame}s with their original source-machine capture timestamps, so the rest of
 * the vision pipeline processes replayed frames identically to live ones.
 *
 * <p><strong>Timestamp contract:</strong> {@code capture_ns} is propagated verbatim through
 * {@link CapturedFrame#captureTimestamp} into {@code Frame.timestampNanos}. The value is
 * opaque source-machine time; consumers (NT publisher, AKit) rebase to replay-machine time if
 * they need to.
 *
 * <p><strong>Calibration / FOV:</strong> recordings carry image data but not intrinsics. FOV
 * is 0 and {@code cameraCalibration} is {@code null} until a calibration is imported via
 * {@code Cameras → Calibration → Import}; pipelines that read {@code horizontalFocalLength}
 * produce nonsense until then.
 *
 * <p><strong>Pacing:</strong> {@link #getInputMat()} blocks the vision thread so wall-clock
 * spacing matches the recording's {@code capture_ns} deltas. Anchored on the first frame and
 * target-based (via {@link System#nanoTime}) so a slow pipeline doesn't accumulate sleep debt.
 *
 * <p><strong>EOF:</strong> the shorter of {@code frames/} and {@code metadata.jsonl} ends
 * replay. Once exhausted, {@link #isConnected()} reports {@code false} and further
 * {@link #getInputMat()} calls return empty {@link CVMat}s.
 *
 * <p><strong>Required structure:</strong> construction throws {@link IOException} if
 * {@code frames/000000.jpg} or {@code metadata.jsonl} is missing. Re-recording via
 * {@link #setRecording} is unsupported on purpose.
 */
public class FileLogFrameProvider extends CpuImageProcessor {
    private final Path recordingDir;
    private final Path framesDir;
    private final MetadataSidecarReader sidecarReader;
    private final FrameStaticProperties properties;
    private final Logger logger;

    // CAS in markExhausted dedupes the "exhausted" log line; a plain volatile would not.
    private final AtomicBoolean exhausted = new AtomicBoolean(false);

    private long emittedFrameCount = 0;

    // Pacing anchor — set on the first emitted frame (see pace()).
    private boolean pacingAnchored = false;
    private long firstCaptureNs;
    private long firstMonoNs;

    /**
     * @param recordingDir directory containing {@code frames/} and {@code metadata.jsonl}, as
     *     written by {@code FrameRecorder}.
     * @throws IOException if either is missing, the first JPEG cannot be decoded, or the sidecar
     *     cannot be read.
     */
    public FileLogFrameProvider(Path recordingDir) throws IOException {
        this.recordingDir = recordingDir;
        this.framesDir = recordingDir.resolve("frames");
        Path metadataPath = recordingDir.resolve("metadata.jsonl");

        if (!Files.isDirectory(framesDir)) {
            throw new IOException(
                    "Recording at " + recordingDir + " is missing the required frames/ directory.");
        }
        if (!Files.isRegularFile(metadataPath)) {
            throw new IOException(
                    "Recording at "
                            + recordingDir
                            + " is missing the required metadata.jsonl sidecar; without source-side"
                            + " capture timestamps the replay timing contract cannot be honoured.");
        }

        // Probe the first frame for static properties; getInputMat re-reads via the sidecar.
        Path firstFrame = FrameLogFormat.framePath(framesDir, 0);
        if (!Files.isRegularFile(firstFrame)) {
            throw new IOException(
                    "Recording at "
                            + recordingDir
                            + " has frames/ but is missing the first frame ("
                            + firstFrame.getFileName()
                            + "). The recording is empty or its file numbering doesn't start at 0.");
        }
        Mat probe = Imgcodecs.imread(firstFrame.toString());
        try {
            if (probe.empty()) {
                throw new IOException(
                        "First frame " + firstFrame + " could not be decoded by Imgcodecs.imread");
            }
            int width = probe.cols();
            int height = probe.rows();
            // FOV / calibration come from a later user-driven import; see class doc.
            this.properties = new FrameStaticProperties(width, height, 0.0, null);

            this.sidecarReader = new MetadataSidecarReader(metadataPath);

            this.logger =
                    new Logger(
                            FileLogFrameProvider.class,
                            recordingDir.getFileName().toString(),
                            LogGroup.Camera);
            this.logger.info(
                    "Opened replay source " + recordingDir + " (" + width + "x" + height + ")");
        } finally {
            probe.release();
        }
    }

    @Override
    public CapturedFrame getInputMat() {
        // Tests / future callers may skip isConnected; fire onCameraConnected defensively so
        // hasConnected() reports the truth.
        if (!cameraPropertiesCached) {
            onCameraConnected();
        }

        if (exhausted.get()) {
            return new CapturedFrame(new CVMat(), properties, 0L);
        }

        Optional<MetadataSidecarReader.Entry> entry;
        try {
            entry = sidecarReader.readNext();
        } catch (IOException e) {
            logger.error(
                    "metadata.jsonl read failed at frame "
                            + emittedFrameCount
                            + "; stopping replay: "
                            + e.getMessage());
            markExhausted("metadata read failure");
            return new CapturedFrame(new CVMat(), properties, 0L);
        }

        if (entry.isEmpty()) {
            markExhausted("metadata.jsonl exhausted after " + emittedFrameCount + " frames");
            return new CapturedFrame(new CVMat(), properties, 0L);
        }

        long seq = entry.get().seq();
        long captureNs = entry.get().captureNs();
        Path framePath = FrameLogFormat.framePath(framesDir, seq);

        // imread returns empty Mat on missing/unreadable — covers the partial-last-frame case
        // (writer flushed jsonl line then crashed mid-imwrite).
        Mat decoded = Imgcodecs.imread(framePath.toString());
        if (decoded.empty()) {
            decoded.release();
            markExhausted(
                    "frame "
                            + framePath.getFileName()
                            + " missing or unreadable (after "
                            + emittedFrameCount
                            + " frames emitted)");
            return new CapturedFrame(new CVMat(), properties, 0L);
        }

        pace(captureNs);

        emittedFrameCount++;
        return new CapturedFrame(new CVMat(decoded), properties, captureNs);
    }

    /** First call anchors; subsequent calls park until target. See class doc for the model. */
    private void pace(long captureNs) {
        if (!pacingAnchored) {
            firstCaptureNs = captureNs;
            firstMonoNs = System.nanoTime();
            pacingAnchored = true;
            return;
        }
        long sleepNs = (firstMonoNs + (captureNs - firstCaptureNs)) - System.nanoTime();
        if (sleepNs > 0) {
            // parkNanos doesn't throw or clear the interrupt flag; the surrounding interrupt
            // handling lives in VisionRunner. Returning early just shortens the park.
            LockSupport.parkNanos(sleepNs);
        }
    }

    /** Static properties probed at construction from the first JPEG (see class doc). */
    public FrameStaticProperties getStaticProperties() {
        return properties;
    }

    private void markExhausted(String reason) {
        if (exhausted.compareAndSet(false, true)) {
            logger.info("Replay source exhausted: " + reason);
        }
    }

    @Override
    public String getName() {
        return "FileLogFrameProvider - " + recordingDir.getFileName();
    }

    @Override
    public void release() {
        try {
            sidecarReader.close();
        } catch (IOException e) {
            logger.warn("Failed to close metadata sidecar reader: " + e.getMessage());
        }
    }

    @Override
    public boolean checkCameraConnected() {
        return !exhausted.get();
    }

    @Override
    public void setRecording(boolean shouldRecord) {
        throw new UnsupportedOperationException(
                "FileLogFrameProvider is a replay source and does not support recording");
    }

    @Override
    public boolean getRecording() {
        return false;
    }
}
