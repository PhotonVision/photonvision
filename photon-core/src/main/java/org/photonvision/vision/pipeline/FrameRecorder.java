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
import org.zeroturnaround.zip.ZipUtil;

/**
 * Writes a directory of JPEG frames ({@code frames/000000.jpg, …}) plus a {@code metadata.jsonl}
 * sidecar of per-frame {@code (seq, capture_ns)} pairs. {@code capture_ns} is the source machine's
 * {@code wpi::nt::Now} epoch at capture, propagated verbatim through replay.
 *
 * <p>Metadata is flushed before its paired frame, so {@code len(metadata.jsonl) >=
 * frame_file_count(frames/)} holds under any process crash; readers truncate jsonl to the frame
 * directory's count. Per-frame fsync is intentionally skipped: it would blow the 33 ms budget on
 * SD-card-class storage.
 */
public class FrameRecorder implements Releasable {
    private static final int QUEUE_CAPACITY = 30; // Buffer up to 30 frames
    private static final long MIN_DISK_SPACE_BYTES = 4L * 1024 * 1024 * 1024; // 4 GB
    private static final String FRAME_FILENAME_FORMAT = "%06d.jpg";

    /** On-disk path of the JPEG at a given sequence number under a recording's frames dir. */
    public static Path framePath(Path framesDir, long seq) {
        return framesDir.resolve(String.format(FRAME_FILENAME_FORMAT, seq));
    }

    private final BlockingQueue<RecordFrame> frameQueue;
    private final Thread writerThread;
    private final AtomicBoolean recording;
    private final AtomicBoolean shutdown;
    private long sequenceCounter = 0;

    public enum RecordingStrategy {
        SNAPSHOTS
    }

    public final RecordingStrategy strat;

    private Logger logger;

    private final Path outputPath;
    private final Path framesDir;

    // 85: ~50-150 KB/frame at 1080p; sufficient detail for AprilTag / retro / colour pipelines.
    private static final int JPEG_QUALITY = 85;

    // Initialised in ctor body after early-throw checks (see release()).
    private final MatOfInt jpegWriteParams;

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

    /**
     * Snapshot of TSS state captured at recording-start. Persisted as {@code tss.json} so the JSON
     * exporter can later place {@code metadata.jsonl}'s local-time-base {@code capture_ns} into the
     * TSS time base.
     */
    public record TssSample(
            boolean tssActiveAtRecord, long tssOffsetAtRecordNs, long sampledAtWpiNtNowNs) {
        /** Sentinel meaning "no TSS info available" — used in tests and when JNI is unavailable. */
        public static final TssSample INACTIVE = new TssSample(false, 0L, 0L);
    }

    public FrameRecorder(Path outputPath) {
        this(
                outputPath,
                HardwareManager.getInstance().getRecordingStrategy(),
                SystemMonitor.getInstance().getUsableDiskSpace(),
                sampleTssNow());
    }

    // Package-private DI constructor: lets tests build a recorder without bringing up the
    // HardwareManager / SystemMonitor singletons (which transitively require photontargetingJNI
    // via NetworkTablesManager → TimeSyncClient). Pass TssSample.INACTIVE when not exercising the
    // tss.json hand-off.
    FrameRecorder(Path outputPath, RecordingStrategy strat, long availableSpace, TssSample tss) {
        this.logger = new Logger(FrameRecorder.class, LogGroup.VisionModule);
        this.strat = strat;

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

        try {
            java.nio.file.Files.createDirectories(framesDir);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create frames directory at " + framesDir, e);
        }

        // Sidecar must open before any frame can be queued — replay needs (seq, capture_ns).
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

        writeTssSnapshot(tss);

        // Past every early-throw — allocate the native MatOfInt last so release() can reach it.
        this.jpegWriteParams = new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, JPEG_QUALITY);

        this.frameQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        this.recording = new AtomicBoolean(false);
        this.shutdown = new AtomicBoolean(false);

        // Start the writer thread
        switch (strat) {
            case SNAPSHOTS -> this.writerThread = new Thread(this::videoLoop, "FrameRecorder-Video");
            default -> throw new IllegalArgumentException("Unsupported Recording Strategy: " + strat);
        }
        this.writerThread.setDaemon(true);
        this.writerThread.start();
    }

    /**
     * Sample TSS state via the live NetworkTablesManager singleton. Returns {@link
     * TssSample#INACTIVE} if photontargetingJNI isn't loaded (tests) or TSS has never seen a pong
     * (no robot up). The 5-second threshold is conservative: NT pings every 1s, so any value over
     * a few seconds means a stale or never-connected client.
     */
    private static TssSample sampleTssNow() {
        try {
            var mgr =
                    org.photonvision.common.dataflow.networktables.NetworkTablesManager.getInstance();
            long timeSinceLastPongUs = mgr.getTimeSinceLastPong();
            boolean active = timeSinceLastPongUs != Long.MAX_VALUE && timeSinceLastPongUs < 5_000_000L;
            long offsetNs = mgr.getOffset() * 1_000L;
            long nowNs = org.wpilib.networktables.NetworkTablesJNI.now() * 1_000L;
            return new TssSample(active, offsetNs, nowNs);
        } catch (Throwable t) {
            // JNI not loaded, NetworkTablesManager not constructed, or TimeSyncClient handle was
            // freed — anything in the stack throws, we treat as inactive.
            return TssSample.INACTIVE;
        }
    }

    /**
     * Write {@code tss.json} alongside {@code metadata.jsonl}. Consumed by {@code
     * JsonResultExporter.readSnapshot} to shift {@code capture_ns} into the TSS time base during
     * AKit replay. Failure is logged-and-swallowed: the recording itself still works, the exporter
     * just falls back to UNKNOWN.
     */
    private void writeTssSnapshot(TssSample tss) {
        Path tssPath = outputPath.resolve("tss.json");
        String json =
                "{\"tss_active_at_record\":"
                        + tss.tssActiveAtRecord()
                        + ",\"tss_offset_at_record_ns\":"
                        + tss.tssOffsetAtRecordNs()
                        + ",\"sampled_at_wpi_nt_now_ns\":"
                        + tss.sampledAtWpiNtNowNs()
                        + "}\n";
        try {
            java.nio.file.Files.write(
                    tssPath,
                    json.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            logger.warn("Failed to write tss snapshot to " + tssPath + ": " + e.getMessage());
        }
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
     * Queue a frame for the writer thread. Clones the input Mat — caller retains ownership of {@code
     * cvmat} and is responsible for releasing it.
     *
     * @param captureTimestampNs source-machine capture time; preserved verbatim into the sidecar.
     * @return false if recording is off, shutting down, the queue is full, or disk is low.
     */
    public boolean recordFrame(CVMat cvmat, long captureTimestampNs) {
        if (!recording.get() || shutdown.get()) {
            return false;
        }

        // Re-check disk every 100 frames; constructor already covered seq=0.
        if (sequenceCounter > 0 && sequenceCounter % 100 == 0) {
            long availableSpace = SystemMonitor.getInstance().getUsableDiskSpace();
            if (availableSpace < MIN_DISK_SPACE_BYTES) {
                logger.error(
                        "Low disk space available ("
                                + availableSpace / (1024 * 1024)
                                + " MB). Stopping FrameRecorder.");
                stopRecording();
                return false;
            }
        }

        Mat clone = cvmat.getMat().clone();
        long seq = sequenceCounter;
        boolean added = frameQueue.offer(new RecordFrame(clone, captureTimestampNs, seq));

        if (!added) {
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

                // Metadata before frame keeps the len(jsonl) >= frame_count invariant (class doc).
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

    /** Stops recording on imwrite failure (disk-full / permissions / encoder error). */
    private void writeFrame(long seq, Mat mat) {
        Path framePath = framePath(framesDir, seq);
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
     * Append one {@code {"seq":N,"capture_ns":T}} line, flushed per-line. Returns false on IO failure
     * (after stopping recording so the invariant isn't broken).
     */
    private boolean writeMetadataLine(long sequenceId, long captureTimestampNs) {
        try {
            metadataWriter.write(
                    "{\"seq\":" + sequenceId + ",\"capture_ns\":" + captureTimestampNs + "}\n");
            metadataWriter.flush();
            return true;
        } catch (java.io.IOException e) {
            logger.error("Failed to write metadata sidecar line; stopping recording: " + e.getMessage());
            stopRecording();
            return false;
        }
    }

    public boolean isRecording() {
        return recording.get();
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

        jpegWriteParams.release();
    }

    /**
     * Export a recording.
     *
     * <p>Image-sequence recordings are directories ({@code frames/*.jpg} + {@code metadata.jsonl} +
     * {@code strat}). For download convenience we zip the whole recording into a single portable
     * file. Users who want a watchable video can run any standard tool against the zipped frame
     * directory (e.g. ffmpeg can build an mp4 from the JPEGs offline).
     *
     * @param recording Path to recording directory
     * @return Path to a unique temp file with the recording's contents zipped
     */
    public static File export(Path recording) throws Exception {
        if (!Files.isDirectory(recording)) {
            throw new IllegalStateException("Recording directory not found: " + recording);
        }
        File zip = Files.createTempFile(recording.getFileName().toString() + "_", ".zip").toFile();
        ZipUtil.pack(recording.toFile(), zip);
        return zip;
    }

    /**
     * Export all recordings under a single camera as a zip. The zip preserves the {@code
     * <recording>/{frames/*.jpg, metadata.jsonl}} tree, so the user can browse recordings by name
     * after unzipping.
     */
    public static File exportCamera(Path cameraRecordingsDir) throws Exception {
        if (!Files.isDirectory(cameraRecordingsDir)) {
            throw new IllegalStateException(
                    "Camera recordings directory not found: " + cameraRecordingsDir);
        }
        String prefix = cameraRecordingsDir.getFileName().toString();
        File zip = Files.createTempFile(prefix + "_recordings_", ".zip").toFile();
        ZipUtil.pack(cameraRecordingsDir.toFile(), zip);
        return zip;
    }

    /**
     * Export every recording from every camera as a single zip. Preserves the {@code
     * <camera>/<recording>/{frames/, metadata.jsonl, strat}} tree so cameras stay grouped.
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
        return List.of(RecordingStrategy.SNAPSHOTS);
    }
}
