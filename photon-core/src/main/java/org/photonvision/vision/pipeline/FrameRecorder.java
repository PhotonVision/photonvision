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

package org.photonvision.vision.pipeline;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.imgcodecs.Imgcodecs;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.hardware.HardwareManager;
import org.photonvision.common.hardware.metrics.SystemMonitor;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.Releasable;
import org.photonvision.vision.processes.VisionModule;
import org.photonvision.vision.processes.VisionSourceManager;
import org.zeroturnaround.zip.ZipUtil;

/**
 * Writes a directory of JPEG frames ({@code frames/000000.jpg, 000001.jpg, …}) plus a
 * {@code metadata.jsonl} sidecar of per-frame {@code (seq, capture_ns)} pairs.
 *
 * <p><strong>Why image-sequence and not a video container:</strong>
 *
 * <ul>
 *   <li><em>No container size cap.</em> AVI's classic 2 GB cap silently corrupts long recordings
 *       (~2 min at 1080p30). MJPEG-AVI / mp4 are container-bound; a frame directory is filesystem-
 *       bound, which on any modern filesystem is "effectively unlimited" for typical match
 *       lengths.
 *   <li><em>Atomic per frame.</em> Each {@code Imgcodecs.imwrite} either produces a complete JPEG
 *       or fails. A crash mid-recording leaves all already-written frames intact and the last
 *       (partial) frame either absent or invalid-but-skippable. No container-index corruption.
 *   <li><em>No codec compatibility risk.</em> JPEG decode via {@code Imgcodecs.imread} is part of
 *       OpenCV core on every supported platform — never depends on a video backend (ffmpeg,
 *       gstreamer, Media Foundation) being present or version-matched. WPILib's Linux OpenCV
 *       ships no video backend, which is what disqualifies H.264 mp4 and made MJPEG-AVI a
 *       workaround; per-file JPEG sidesteps the whole question.
 *   <li><em>Random seek.</em> Replay can jump to any frame N by opening {@code N.jpg} directly,
 *       enabling future Phase 3 batch / out-of-order replay without recoding the format.
 *   <li><em>Truncatable.</em> Readers handle a partial recording by counting files; no extra
 *       work to recover from a writer crash.
 * </ul>
 *
 * <p>Trade-off vs. a video container: ~8-10× larger than H.264 (each frame is an intra-frame
 * JPEG, no temporal compression), plus filesystem inode pressure (~1800 files / minute at 30 fps).
 * At 1080p30 the per-recording size is ~1 GB/minute. ext4 / NTFS / eMMC handle the file count
 * fine; FAT32 starts to slow down past ~10k files in one directory. If that becomes a problem,
 * shard frames into subdirectories ({@code frames/000/}, {@code frames/001/}) — for now, flat.
 *
 * <p><strong>How recordings are triggered:</strong> the web UI's Recordings card and robot code
 * (via {@code PhotonCamera.setRecording(bool)} in photonlib) both write to the per-camera NT
 * boolean {@code /photonvision/<nickname>/recordingRequest}. PhotonVision's {@code NTDataPublisher}
 * subscribes to that topic and calls {@code frameProvider.setRecording(bool)} on the matching
 * {@code VisionModule}, which in turn constructs / releases this {@code FrameRecorder}. Robot
 * programmers should drive it on game-event edges rather than continuously — recordings are
 * disk-expensive at 1 GB/minute.
 *
 * <p>{@code capture_ns} is the source machine's {@code wpi::nt::Now} epoch at capture, recorded
 * verbatim. Replay readers must propagate it through {@code Frame.timestampNanos} unchanged —
 * the replay machine's clock is irrelevant; downstream consumers (NT publisher, AKit) treat it
 * as opaque time and rebase if they need to.
 *
 * <p>Metadata is flushed before its paired frame, so under any crash
 * {@code len(metadata.jsonl) >= frame_file_count(frames/)}. Readers truncate jsonl to the frame
 * directory's count, and ignore unknown JSON fields so the schema can grow (exposure, gain,
 * calibration version) without breaking older readers.
 */
public class FrameRecorder implements Releasable {
    private static final int QUEUE_CAPACITY = 30; // Buffer up to 30 frames
    private static final long MIN_DISK_SPACE_BYTES = 4L * 1024 * 1024 * 1024; // 4 GB

    private final BlockingQueue<RecordFrame> frameQueue;
    private final Thread writerThread;
    private final AtomicBoolean recording;
    private final AtomicBoolean shutdown;
    private long sequenceCounter = 0;

    public enum RecordingStrategy {
        VIDEO
    }

    public final RecordingStrategy strat;

    private Logger logger;

    private final Path outputPath;
    private final Path framesDir;

    // JPEG quality (1-100). 85 is the visual / size sweet spot for the kind of content PV's
    // pipelines actually consume (AprilTags, retro-reflective targets, coloured shapes). At
    // 1080p that's ~50-150 KB per frame depending on detail. If a future use case wants
    // pristine pixels, configure higher; for storage-constrained use, drop to ~70.
    private static final int JPEG_QUALITY = 85;

    // Reused JPEG-quality param vector for Imgcodecs.imwrite. Allocated once per recorder.
    private final MatOfInt jpegWriteParams =
            new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, JPEG_QUALITY);

    // 6 digits supports up to 1M frames per recording — ~9 hours @ 30 fps, ~2.3 hours @ 120 fps,
    // both well beyond any realistic match recording. Zero-padded so lexical sort matches numeric
    // sort, which is what replay's directory iteration relies on.
    private static final String FRAME_FILENAME_FORMAT = "%06d.jpg";

    // Per-frame metadata sidecar, one JSON object per line. Encoder-agnostic so it survives
    // format switches — filenames no longer carry per-frame info, sidecar is the only
    // source of seq + capture timing for replay.
    private final BufferedWriter metadataWriter;

    private static class RecordFrame {
        final Mat mat;
        final long captureTimestampNs;
        final long sequenceId;
        final boolean isPoison;

        RecordFrame(Mat mat, long captureTimestampNs, long sequenceId) {
            this.mat = mat;
            this.captureTimestampNs = captureTimestampNs;
            this.sequenceId = sequenceId;
            this.isPoison = false;
        }

        // Poison pill to signal shutdown
        private RecordFrame() {
            this.mat = null;
            this.captureTimestampNs = 0;
            this.sequenceId = 0;
            this.isPoison = true;
        }

        static RecordFrame poison() {
            return new RecordFrame();
        }
    }

    public FrameRecorder(Path outputPath) {
        this(
                outputPath,
                HardwareManager.getInstance().getRecordingStrategy(),
                SystemMonitor.getInstance().getUsableDiskSpace());
    }

    // Package-private DI constructor: lets tests build a recorder without bringing up the
    // HardwareManager / SystemMonitor singletons (which transitively require photontargetingJNI
    // via NetworkTablesManager → TimeSyncClient).
    FrameRecorder(Path outputPath, RecordingStrategy strat, long availableSpace) {
        this.logger = new Logger(FrameRecorder.class, LogGroup.VisionModule);
        this.strat = strat;

        // Check if we're under 4 GB of available space, if so exit
        if (availableSpace < MIN_DISK_SPACE_BYTES) {
            logger.error(
                    "Low disk space available ("
                            + availableSpace / (1024 * 1024)
                            + " MB). FrameRecorder will not start.");
            throw new IllegalStateException("Insufficient disk space for FrameRecorder");
        }

        logger.info("Initializing FrameRecorder with output path: " + outputPath.toString());
        this.outputPath = outputPath;
        this.framesDir = outputPath.resolve("frames");

        // Write strategy to a file in the output path
        try {
            // Ensure output + frames directory exist. createDirectories is no-op if present.
            java.nio.file.Files.createDirectories(framesDir);

            java.nio.file.Path strategyFile = outputPath.resolve("strat");
            String content = strat.name() + System.lineSeparator();

            java.nio.file.Files.write(
                    strategyFile,
                    content.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            logger.warn(
                    "Failed to write recording strategy file to " + outputPath + ": " + e.getMessage());
        }

        // Open the metadata sidecar. Required for replay — fatal at construction so we don't
        // produce a frame directory missing the seq + capture_ns needed to feed frames back
        // through the pipeline.
        try {
            this.metadataWriter =
                    java.nio.file.Files.newBufferedWriter(
                            outputPath.resolve("metadata.jsonl"),
                            java.nio.charset.StandardCharsets.UTF_8,
                            java.nio.file.StandardOpenOption.CREATE,
                            java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to open metadata sidecar at " + outputPath + "/metadata.jsonl", e);
        }

        this.frameQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        this.recording = new AtomicBoolean(false);
        this.shutdown = new AtomicBoolean(false);

        // Start the writer thread
        switch (strat) {
            case VIDEO ->
                    this.writerThread = new Thread(this::videoLoop, "FrameRecorder-Video");
            default -> throw new IllegalArgumentException("Unsupported Recording Strategy: " + strat);
        }
        this.writerThread.setDaemon(true);
        this.writerThread.start();
    }

    /** Start recording. Frames offered after this point will be written to {@code frames/}. */
    public boolean startRecording() {
        if (recording.get()) {
            return false;
        }

        recording.set(true);
        return true;
    }

    /** Stop recording. Frames already in the queue are still flushed by the writer thread. */
    public void stopRecording() {
        if (!recording.get()) {
            return;
        }

        recording.set(false);
    }

    /**
     * Record a frame. Non-blocking: clones the input Mat and queues the clone, so the caller retains
     * ownership of the passed CVMat and is responsible for releasing it. If the queue is full, the
     * new frame is dropped (existing queued frames are kept) and the clone is released internally.
     *
     * @param cvmat The frame to record. Not retained or released by this method.
     * @param captureTimestampNs Wall-clock nanoseconds at which the camera captured this frame
     *     (typically from cscore's grabFrame, in the wpi::nt::Now epoch). Used for both the on-disk
     *     filename and any downstream replay timing; the value is preserved verbatim and never
     *     replaced with a write-time clock read.
     * @return true if frame was queued, false if recording is not active, queue is full, or we've run
     *     out of disk space.
     */
    public boolean recordFrame(CVMat cvmat, long captureTimestampNs) {
        if (!recording.get() || shutdown.get()) {
            return false;
        }

        // Skip seq=0: the constructor already checked initial disk space, so the
        // redundant check would only burn SystemMonitor latency on the first-frame path.
        if (sequenceCounter > 0 && sequenceCounter % 100 == 0) {
            double availableSpace = SystemMonitor.getInstance().getUsableDiskSpace();

            // Check if we're under 4 GB of available space, if so stop recording
            if (availableSpace < MIN_DISK_SPACE_BYTES) {
                logger.error(
                        "Low disk space available ("
                                + availableSpace / (1024 * 1024)
                                + " MB). Stopping FrameRecorder.");
                stopRecording();
                return false;
            }
        }

        // Clone so the caller's Mat is independent of ours: the pipeline downstream will release
        // the original after processing, and the writer thread will release this clone after write.
        Mat clone = cvmat.getMat().clone();
        long seq = sequenceCounter;
        boolean added = frameQueue.offer(new RecordFrame(clone, captureTimestampNs, seq));

        if (!added) {
            // Queue full; release the clone we made (not the caller's Mat).
            clone.release();
        }

        sequenceCounter++;

        return added;
    }

    /** Worker thread: encodes each BGR frame as a standalone JPEG into the frames directory. */
    private void videoLoop() {
        while (!shutdown.get()) {
            try {
                RecordFrame frame = frameQueue.take();

                if (frame.isPoison) {
                    break;
                }

                // Metadata before frame: keeps len(jsonl) >= frame_count(frames/) under any
                // crash, so replay readers truncate jsonl to the file count — never the other
                // way around. If metadata write fails, skip the frame to preserve the invariant
                // (writeMetadataLine stops recording on failure).
                if (writeMetadataLine(frame.sequenceId, frame.captureTimestampNs)) {
                    writeFrame(frame.sequenceId, frame.mat);
                }

                frame.mat.release();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Write one BGR frame as a standalone JPEG into {@code frames/<seq>.jpg}.
     *
     * <p>Each call is independent and atomic at the OS layer — a partial write produces an
     * invalid JPEG that replay skips, not a corrupted container. {@code Imgcodecs.imwrite}
     * returns false on disk-full / permissions / encoder failure; we log and stop recording so
     * the writer thread doesn't burn cycles draining the queue past a broken disk.
     */
    private void writeFrame(long seq, Mat mat) {
        Path framePath = framesDir.resolve(String.format(FRAME_FILENAME_FORMAT, seq));
        boolean ok = Imgcodecs.imwrite(framePath.toString(), mat, jpegWriteParams);
        if (!ok) {
            logger.error(
                    "Imgcodecs.imwrite failed for "
                            + framePath
                            + " (disk full? permission denied? encoder error?); stopping recording.");
            stopRecording();
        }
    }

    /**
     * Write one JSONL line: {"seq":N,"capture_ns":T}. Flushed per-line so a crash doesn't lose
     * the last few frames of metadata. Manually formatted (no Jackson) because the schema is
     * trivial and this is on the writer thread's hot path.
     *
     * @return true on success; false on IO failure (after stopping the recording — caller must
     *     not write the paired frame, or the jsonl-≥-mp4 invariant breaks).
     */
    private boolean writeMetadataLine(long sequenceId, long captureTimestampNs) {
        try {
            metadataWriter.write(
                    "{\"seq\":" + sequenceId + ",\"capture_ns\":" + captureTimestampNs + "}\n");
            metadataWriter.flush();
            return true;
        } catch (java.io.IOException e) {
            logger.error(
                    "Failed to write metadata sidecar line; stopping recording: " + e.getMessage());
            stopRecording();
            return false;
        }
    }

    public boolean isRecording() {
        return recording.get();
    }

    public int getQueueSize() {
        return frameQueue.size();
    }

    @Override
    public void release() {
        if (shutdown.getAndSet(true)) {
            return; // Already released
        }

        stopRecording();

        // Send poison pill to stop writer thread
        try {
            frameQueue.put(RecordFrame.poison());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            writerThread.join(1000); // Wait up to 1 second
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Clear any remaining frames
        RecordFrame frame;
        while ((frame = frameQueue.poll()) != null) {
            if (frame.mat != null) {
                frame.mat.release();
            }
        }

        try {
            metadataWriter.close();
        } catch (java.io.IOException e) {
            logger.warn("Failed to close metadata sidecar: " + e.getMessage());
        }

        // jpegWriteParams is a small MatOfInt allocated once at construction. GC handles it,
        // but explicit release matches the rest of the class's resource discipline.
        jpegWriteParams.release();
    }

    /**
     * Export a recording.
     *
     * <p>Image-sequence recordings are directories ({@code frames/*.jpg} + {@code metadata.jsonl}
     * + {@code strat}). For download convenience we zip the whole recording into a single
     * portable file. Users who want a watchable video can run any standard tool against the
     * zipped frame directory (e.g. ffmpeg can build an mp4 from the JPEGs offline).
     *
     * @param recording Path to recording directory
     * @return Path to a unique temp file with the recording's contents zipped
     */
    public static File export(Path recording) throws Exception {
        if (!Files.isDirectory(recording)) {
            throw new IllegalStateException("Recording directory not found: " + recording);
        }
        File zip =
                Files.createTempFile(recording.getFileName().toString() + "_", ".zip").toFile();
        ZipUtil.pack(recording.toFile(), zip);
        return zip;
    }

    /**
     * Export all recordings under a single camera as a zip. The zip preserves the
     * {@code <recording>/{frames/*.jpg, metadata.jsonl, strat}} tree, so the user can browse
     * recordings by name after unzipping.
     */
    public static File exportCamera(Path cameraRecordingsDir) throws Exception {
        if (!Files.isDirectory(cameraRecordingsDir)) {
            throw new IllegalStateException(
                    "Camera recordings directory not found: " + cameraRecordingsDir);
        }
        // Use the camera's nickname for the zip filename if we can find it; fall back to the
        // directory's own name. The original code threw NPE when the camera config was missing;
        // tolerating that here means a user can still export recordings from a camera they've
        // since unassigned.
        String prefix =
                VisionSourceManager.getInstance().getVisionModules().stream()
                        .filter(
                                module ->
                                        module
                                                .getCameraConfiguration()
                                                .uniqueName
                                                .equals(cameraRecordingsDir.getFileName().toString()))
                        .findFirst()
                        .map(module -> module.getCameraConfiguration().nickname)
                        .orElseGet(() -> cameraRecordingsDir.getFileName().toString());
        File zip = Files.createTempFile(prefix + "_recordings_", ".zip").toFile();
        ZipUtil.pack(cameraRecordingsDir.toFile(), zip);
        return zip;
    }

    /**
     * Export every recording from every camera as a single zip. Preserves the
     * {@code <camera>/<recording>/{frames/, metadata.jsonl, strat}} tree so cameras stay
     * grouped.
     */
    public static File exportAll() throws Exception {
        Path recordingsRoot = ConfigManager.getInstance().getRecordingsDirectory().toPath();
        File zip = Files.createTempFile("photonvision-recordings-export-", ".zip").toFile();
        if (Files.isDirectory(recordingsRoot)) {
            ZipUtil.pack(recordingsRoot.toFile(), zip);
        }
        return zip;
    }

    public static List<RecordingStrategy> getSupportedStrategies() {
        return List.of(RecordingStrategy.VIDEO);
    }
}
