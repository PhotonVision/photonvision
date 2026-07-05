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
import java.util.function.LongConsumer;
import java.util.stream.Stream;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipeline.FrameRecorder;

/**
 * Replay-side counterpart to {@link org.photonvision.vision.pipeline.FrameRecorder}. Reads a
 * recording directory ({@code frames/<seq>.jpg} + {@code metadata.jsonl}) and emits {@link
 * CapturedFrame}s with their original {@code capture_ns} stamped onto {@code Frame.timestampNanos}.
 * {@link #getInputMat()} blocks the vision thread so wall-clock spacing matches the recording's
 * {@code capture_ns} deltas.
 *
 * <p>At EOF the provider transitions to its stopped state and fires {@link #setOnEof onEof} once.
 * Swap-aware consumers (see {@code VisionModule.startReplay}) wire the callback and receive empty
 * frames after that; standalone callers (no callback) get the legacy park-until-interrupted
 * behaviour so they don't busy-loop. Construction throws {@link IOException} if either {@code
 * frames/000000.jpg} or {@code metadata.jsonl} is missing.
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

    // The thread currently parked in pace(), if any. Volatile so requestStop() (any thread) can
    // unpark it instead of waiting out the full recorded inter-frame gap.
    private volatile Thread pacingThread;

    // Discovered at construction by listing frames/*.jpg. Used by replay UIs to render a
    // progress bar without having to walk the directory themselves. -1 if discovery failed
    // (logged once, treated as unknown).
    private final long totalFrames;

    // Optional hooks for replay orchestration on the consumer side. Volatile so a controller
    // thread that wires them after construction is visible to the vision thread on next read.
    private volatile LongConsumer onProgress;
    private volatile Runnable onEof;

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

        Path firstFrame = FrameRecorder.framePath(framesDir, 0);
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

            this.totalFrames = countFrames(framesDir, this.logger);
        } finally {
            probe.release();
        }
    }

    private static long countFrames(Path framesDir, Logger logger) {
        try (Stream<Path> entries = Files.list(framesDir)) {
            return entries.filter(p -> p.getFileName().toString().endsWith(".jpg")).count();
        } catch (IOException e) {
            logger.warn("Failed to count frames in " + framesDir + ": " + e.getMessage());
            return -1;
        }
    }

    @Override
    public CapturedFrame getInputMat() {
        if (!cameraPropertiesCached) {
            onCameraConnected();
        }

        if (stoppedAtEof.get()) {
            return emptyOrPark();
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
        Path framePath = FrameRecorder.framePath(framesDir, seq);

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
        LongConsumer progress = onProgress;
        if (progress != null) {
            try {
                progress.accept(emittedFrameCount);
            } catch (Throwable t) {
                // Never let a misbehaving consumer break the vision thread.
                logger.error("onProgress callback threw", t);
            }
        }
        return new CapturedFrame(new CVMat(decoded), properties, captureNs);
    }

    private void pace(long captureNs) {
        if (!pacingAnchored) {
            firstCaptureNs = captureNs;
            firstMonoNs = System.nanoTime();
            pacingAnchored = true;
            return;
        }
        long deadlineNs = firstMonoNs + (captureNs - firstCaptureNs);
        pacingThread = Thread.currentThread();
        try {
            long sleepNs;
            // Loop: parkNanos can return spuriously, and requestStop() unparks us early — re-check
            // the deadline and the stop flag each wakeup. Preserve the interrupt flag for the
            // runner's outer loop.
            while ((sleepNs = deadlineNs - System.nanoTime()) > 0
                    && !stoppedAtEof.get()
                    && !Thread.currentThread().isInterrupted()) {
                LockSupport.parkNanos(sleepNs);
            }
        } finally {
            pacingThread = null;
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

    /**
     * @return total frame count discovered at construction (count of {@code frames/*.jpg}), or {@code
     *     -1} if discovery failed.
     */
    public long getTotalFrames() {
        return totalFrames;
    }

    /** Fired on the vision thread after each successfully-emitted frame with the running count. */
    public void setOnProgress(LongConsumer onProgress) {
        this.onProgress = onProgress;
    }

    /**
     * Fired once when the provider transitions to its stopped state — either because the recording
     * was exhausted naturally or because {@link #requestStop()} was called. Runs on whichever thread
     * tripped the transition; the implementation should not block.
     *
     * <p><strong>Wire BEFORE installing this provider on a VisionSource.</strong> The natural-EOF
     * path fires from inside the very first {@link #getInputMat()} call on a zero-frame (or
     * already-corrupt) recording, so a callback wired after the runner has begun pulling frames could
     * be skipped silently — the swap-back would never run. {@code VisionModule.startReplay} preserves
     * this ordering; future callers must do the same.
     */
    public void setOnEof(Runnable onEof) {
        this.onEof = onEof;
    }

    /**
     * Trigger an early end-of-replay from any thread. Idempotent. Once called, the next call to
     * {@link #getInputMat()} returns an empty frame without parking (assuming {@link #setOnEof} has
     * been wired) or parks (standalone use). Fires the EOF callback exactly once via the same CAS the
     * natural-EOF path uses.
     */
    public void requestStop() {
        enterStoppedFiringEofOnce("explicit cancel");
        LockSupport.unpark(pacingThread);
    }

    private CapturedFrame enterStoppedState(String reason) {
        enterStoppedFiringEofOnce(reason);
        return emptyOrPark();
    }

    private void enterStoppedFiringEofOnce(String reason) {
        if (!stoppedAtEof.compareAndSet(false, true)) return;
        logger.info("Replay reached end of recording: " + reason);
        Runnable cb = onEof;
        if (cb == null) return;
        try {
            cb.run();
        } catch (Throwable t) {
            logger.error("onEof callback threw", t);
        }
    }

    // Swap-aware consumers (setOnEof wired): return an empty frame so the runner moves on.
    // Standalone: park until interrupted, matching the legacy "deactivate to restart" semantics.
    private CapturedFrame emptyOrPark() {
        if (onEof == null) {
            while (!Thread.currentThread().isInterrupted()) {
                LockSupport.parkNanos(Long.MAX_VALUE);
            }
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
}
