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
 * recording directory ({@code frames/<seq>.jpg} + {@code metadata.jsonl}) and emits {@link
 * CapturedFrame}s with their original source-machine capture timestamps so the rest of the vision
 * pipeline processes replayed frames identically to live ones.
 *
 * <p><strong>Timestamp contract:</strong> {@code capture_ns} is propagated verbatim through {@link
 * CapturedFrame#captureTimestamp} into {@code Frame.timestampNanos}. The value is opaque
 * source-machine time; consumers rebase to replay-machine time if they need to.
 *
 * <p><strong>Calibration / FOV:</strong> recordings carry image data but not intrinsics. FOV is 0
 * and {@code cameraCalibration} is {@code null} until the user imports one via {@code Cameras →
 * Calibration → Import}; pipelines that read {@code horizontalFocalLength} produce nonsense until
 * then.
 *
 * <p><strong>Pacing:</strong> {@link #getInputMat()} blocks the vision thread so wall-clock spacing
 * matches the recording's {@code capture_ns} deltas. Target-based via {@link System#nanoTime} so a
 * slow pipeline doesn't accumulate sleep debt.
 *
 * <p><strong>EOF:</strong> the shorter of {@code frames/} and {@code metadata.jsonl} ends the
 * linear pass; {@link #getInputMat} then parks the vision thread until interrupted (the
 * deactivation path). No further pipeline runs, no NT updates, no MJPEG frames pushed — the browser
 * holds the last delivered MJPEG part. {@link #isConnected} stays {@code true} so the camera
 * remains Active in the UI; to restart from frame 0 the user deactivates and reactivates.
 *
 * <p><strong>Settings-during-EOF caveat:</strong> pipeline settings changes submitted while the
 * vision thread is parked do not apply or persist — settings-event processing runs at the top of
 * {@code VisionRunner}'s loop, which is blocked inside {@link #getInputMat}, and queued events are
 * dropped on interrupt. Change settings during the linear pass, or deactivate first, change them,
 * then reactivate.
 *
 * <p><strong>Required structure:</strong> construction throws {@link IOException} if {@code
 * frames/000000.jpg} or {@code metadata.jsonl} is missing. Re-recording via {@link #setRecording}
 * is unsupported on purpose.
 */
public class FileLogFrameProvider extends CpuImageProcessor {
    private final Path recordingDir;
    private final Path framesDir;
    private final MetadataSidecarReader sidecarReader;
    private final FrameStaticProperties properties;
    private final Logger logger;

    private final AtomicBoolean stoppedAtEof = new AtomicBoolean(false);

    private long emittedFrameCount = 0;

    private boolean pacingAnchored = false;
    private long firstCaptureNs;
    private long firstMonoNs;

    /**
     * @param recordingDir directory containing {@code frames/} and {@code metadata.jsonl}, as written
     *     by {@code FrameRecorder}.
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
            this.properties = new FrameStaticProperties(width, height, 0.0, null);

            this.sidecarReader = new MetadataSidecarReader(metadataPath);

            this.logger =
                    new Logger(
                            FileLogFrameProvider.class, recordingDir.getFileName().toString(), LogGroup.Camera);
            this.logger.info("Opened replay source " + recordingDir + " (" + width + "x" + height + ")");
        } finally {
            probe.release();
        }
    }

    @Override
    public CapturedFrame getInputMat() {
        if (!cameraPropertiesCached) {
            onCameraConnected();
        }

        if (stoppedAtEof.get()) {
            return parkUntilInterrupted();
        }

        Optional<MetadataSidecarReader.Entry> entry;
        try {
            entry = sidecarReader.readNext();
        } catch (IOException e) {
            logger.error(
                    "metadata.jsonl read failed at frame "
                            + emittedFrameCount
                            + "; stopping at EOF: "
                            + e.getMessage());
            return enterStoppedState("metadata read failure");
        }

        if (entry.isEmpty()) {
            return enterStoppedState("metadata.jsonl exhausted after " + emittedFrameCount + " frames");
        }

        long seq = entry.get().seq();
        long captureNs = entry.get().captureNs();
        Path framePath = FrameLogFormat.framePath(framesDir, seq);

        // imread returns empty Mat on missing/unreadable — covers the partial-last-frame case
        // where the writer flushed a jsonl line then crashed mid-imwrite.
        Mat decoded = Imgcodecs.imread(framePath.toString());
        if (decoded.empty()) {
            decoded.release();
            return enterStoppedState(
                    "frame "
                            + framePath.getFileName()
                            + " missing or unreadable (after "
                            + emittedFrameCount
                            + " frames emitted)");
        }

        pace(captureNs);

        emittedFrameCount++;
        return new CapturedFrame(new CVMat(decoded), properties, captureNs);
    }

    private void pace(long captureNs) {
        if (!pacingAnchored) {
            firstCaptureNs = captureNs;
            firstMonoNs = System.nanoTime();
            pacingAnchored = true;
            return;
        }
        long sleepNs = (firstMonoNs + (captureNs - firstCaptureNs)) - System.nanoTime();
        if (sleepNs > 0) {
            LockSupport.parkNanos(sleepNs);
        }
    }

    public FrameStaticProperties getStaticProperties() {
        return properties;
    }

    /**
     * @return the recording directory this provider was constructed against (the parent of {@code
     *     frames/} and {@code metadata.jsonl}). Used by {@code VisionSource.getReplayRecordingDir()}
     *     to surface the current replay target without exposing the field directly.
     */
    public Path getRecordingDir() {
        return recordingDir;
    }

    private CapturedFrame enterStoppedState(String reason) {
        if (stoppedAtEof.compareAndSet(false, true)) {
            logger.info("Replay reached end of recording: " + reason);
        }
        return parkUntilInterrupted();
    }

    private CapturedFrame parkUntilInterrupted() {
        while (!Thread.currentThread().isInterrupted()) {
            LockSupport.parkNanos(Long.MAX_VALUE);
        }
        return new CapturedFrame(new CVMat(), properties, 0L);
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
        return true;
    }

    @Override
    public void setRecording(boolean shouldRecord) {
        // Must not throw: NTDataPublisher invokes this on the NT listener thread with no
        // try/catch around the consumer, so an unchecked exception poisons NT4's listener pool.
        if (shouldRecord) {
            logger.warn("Ignoring setRecording(true): file-log replay is read-only.");
        }
    }

    @Override
    public boolean getRecording() {
        return false;
    }
}
