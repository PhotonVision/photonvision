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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;
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
 * Writes a Motion-JPEG AVI ({@code recording.avi}) plus a {@code metadata.jsonl} sidecar of
 * per-frame {@code (seq, capture_ns)} pairs.
 *
 * <p><strong>Why MJPEG-AVI and not H.264 mp4:</strong> the on-disk format must be decodable by
 * PhotonVision's bundled OpenCV on every platform we ship (Windows x64, Linux x64/arm64). The
 * WPILib OpenCV 4.10.0-3 build links no ffmpeg / gstreamer plugin on Linux — {@code VideoCapture}
 * cannot decode mp4 there. CV_MJPEG (Motion-JPEG-in-AVI, FourCC {@code MJPG}) is the only video
 * format compiled directly into OpenCV's videoio module on every build, so a recording written
 * with {@code VideoWriter(MJPG)} can be read back via {@code VideoCapture(CAP_ANY)} on any host
 * we support. Trade-off: ~8–10× larger files than H.264 (full intra-frame coding, each frame is
 * an independent JPEG). At 1080p30 expect ~1 GB/min; bring a hard drive.
 *
 * <p><strong>Single-file 2 GB cap:</strong> the classic AVI container limit. At 1080p30 that's
 * ~2 minutes; at 720p30 ~5 minutes. Documented Phase 1 limit — chunking is a follow-up.
 *
 * <p><strong>How recordings are triggered:</strong> the web UI's Recordings card and robot code
 * (via {@code PhotonCamera.setRecording(bool)} in photonlib) both write to the per-camera NT
 * boolean {@code /photonvision/<nickname>/recordingRequest}. PhotonVision's {@code NTDataPublisher}
 * subscribes to that topic and calls {@code frameProvider.setRecording(bool)} on the matching
 * {@code VisionModule}, which in turn constructs / releases this {@code FrameRecorder}. Robot
 * programmers should drive it on game-event edges rather than continuously — recordings are
 * expensive at MJPEG-AVI sizes.
 *
 * <p>{@code capture_ns} is the source machine's {@code wpi::nt::Now} epoch at capture, recorded
 * verbatim. Replay readers must propagate it through {@code Frame.timestampNanos} unchanged —
 * the replay machine's clock is irrelevant; downstream consumers (NT publisher, AKit) treat it
 * as opaque time and rebase if they need to.
 *
 * <p>Metadata is flushed before its paired frame, so under any crash
 * {@code len(metadata.jsonl) >= decoded_frame_count(recording.avi)}. Readers truncate jsonl to
 * the avi's decoded count, and ignore unknown JSON fields so the schema can grow (exposure,
 * gain, calibration version) without breaking older readers.
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
    private final Path outputVideoPath;

    // OpenCV VideoWriter that encodes each BGR frame to JPEG and packs it into a single AVI
    // container. Opened lazily on the first recordFrame call so we have the frame dimensions
    // for the writer's Size argument (VideoWriter requires a fixed resolution per-file).
    private VideoWriter videoWriter;

    // JPEG quality (1-100) applied to each frame inside the MJPEG-AVI. 85 is the visual /
    // size sweet spot for the kind of content PV's pipelines actually consume (AprilTags,
    // retro-reflective targets, coloured shapes) and keeps file sizes around 250 KB/frame
    // at 1080p — well inside the 2 GB single-file AVI cap for typical match-length recordings.
    private static final int JPEG_QUALITY = 85;

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
        this.outputVideoPath = outputPath.resolve("recording.avi");

        // Write strategy to a file in the output path
        try {
            // Ensure output directory exists
            java.nio.file.Files.createDirectories(outputPath);

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
        // produce a recording.avi missing the seq + capture_ns needed to feed frames back
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

    /** Start recording. This initializes the VideoWriter. */
    public boolean startRecording() {
        if (recording.get()) {
            return false;
        }

        recording.set(true);
        return true;
    }

    /** Stop recording. Flushes remaining frames and closes the VideoWriter. */
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

    /** Worker thread: encodes each BGR frame as a JPEG inside the MJPEG-AVI container. */
    private void videoLoop() {
        while (!shutdown.get()) {
            try {
                RecordFrame frame = frameQueue.take();

                if (frame.isPoison) {
                    break;
                }

                if (videoWriter == null) {
                    if (!openVideoWriter(frame.mat.cols(), frame.mat.rows())) {
                        frame.mat.release();
                        // Writer couldn't open; stop recording so we don't busy-drain the queue.
                        stopRecording();
                        continue;
                    }
                }

                // Metadata before frame: keeps len(jsonl) >= avi frame count under any crash,
                // so replay readers truncate jsonl to the avi's decoded count — never the
                // other way around. If metadata write fails, skip the frame to preserve the
                // invariant (writeMetadataLine stops recording on failure).
                if (writeMetadataLine(frame.sequenceId, frame.captureTimestampNs)) {
                    videoWriter.write(frame.mat);
                }

                frame.mat.release();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Open the OpenCV {@link VideoWriter} that encodes each BGR frame as JPEG into an MJPEG
     * AVI. Lazy because we need the frame's dimensions before opening — the writer requires a
     * fixed resolution.
     *
     * <p>FourCC {@code MJPG} is the only video format compiled directly into WPILib's bundled
     * OpenCV on every platform — neither ffmpeg nor gstreamer plugins ship in the Linux build.
     * Decode on replay therefore needs no runtime deps beyond what we already require.
     *
     * @return true on success, false if {@code VideoWriter} couldn't open the file or codec
     *     (filesystem error, missing parent directory, etc.) — caller should stop recording.
     */
    private boolean openVideoWriter(int width, int height) {
        int fourcc = VideoWriter.fourcc('M', 'J', 'P', 'G');
        videoWriter =
                new VideoWriter(outputVideoPath.toString(), fourcc, 30.0, new Size(width, height));
        if (!videoWriter.isOpened()) {
            logger.error(
                    "VideoWriter could not open "
                            + outputVideoPath
                            + " (codec=MJPG, "
                            + width
                            + "x"
                            + height
                            + "). The OpenCV build may lack CV_MJPEG — this should not happen on a "
                            + "stock WPILib OpenCV.");
            videoWriter.release();
            videoWriter = null;
            return false;
        }
        // q=85: visual sweep spot for our pipeline content; controllable via the VideoWriter prop.
        videoWriter.set(Videoio.VIDEOWRITER_PROP_QUALITY, JPEG_QUALITY);
        logger.info(
                "Opened MJPEG-AVI writer for "
                        + width
                        + "x"
                        + height
                        + " @ q"
                        + JPEG_QUALITY
                        + " -> "
                        + outputVideoPath);
        return true;
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

        // Close the VideoWriter to finalize the AVI (write the index chunk). VideoWriter.release
        // is synchronous and will block until the file is fully flushed.
        if (videoWriter != null) {
            videoWriter.release();
            videoWriter = null;
        }
    }

    /**
     * Export a recording.
     *
     * <p>Returns a temp copy of {@code <recording>/recording.avi} so callers (single-recording
     * export, exportCamera, exportAll) can safely move/zip without disturbing the original.
     *
     * @param recording Path to recording directory
     * @return Path to a unique temp file with the exported .avi
     */
    public static File export(Path recording) throws Exception {
        Path avi = recording.resolve("recording.avi");
        if (!Files.exists(avi)) {
            throw new IllegalStateException("No recording.avi found in " + recording);
        }
        // Prefix with the recording's dir name so exportCamera's ZIP entries are unique.
        File copy =
                Files.createTempFile(recording.getFileName().toString() + "_", ".avi").toFile();
        Files.copy(avi, copy.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return copy;
    }

    public static File exportCamera(Path cameraRecordingsDir) throws Exception {
        File[] exportedRecordings;
        try (var stream = Files.list(cameraRecordingsDir)) {
            exportedRecordings =
                    stream
                            .map(
                                    path -> {
                                        try {
                                            return FrameRecorder.export(path);
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                    })
                            .toArray(File[]::new);
        }

        // Create a zip of all exported recordings
        File zipPath =
                Files.createTempFile(
                                VisionSourceManager.getInstance().getVisionModules().stream()
                                                .filter(
                                                        module ->
                                                                module
                                                                        .getCameraConfiguration()
                                                                        .uniqueName
                                                                        .matches(cameraRecordingsDir.getFileName().toString()))
                                                .findFirst()
                                                .get()
                                                .getCameraConfiguration()
                                                .nickname
                                        + "_recordings",
                                ".zip")
                        .toFile();

        ZipUtil.packEntries(exportedRecordings, zipPath);

        return zipPath;
    }

    public static File exportAll() throws Exception {
        List<File> exportedRecordings = new ArrayList<>();
        for (VisionModule module : VisionSourceManager.getInstance().getVisionModules()) {
            Path dir =
                    ConfigManager.getInstance()
                            .getRecordingsDirectory()
                            .toPath()
                            .resolve(module.getCameraConfiguration().uniqueName);

            if (!Files.exists(dir)) {
                continue;
            }

            Path camExportDir = Files.createTempDirectory(module.getCameraConfiguration().nickname);

            try (var recordings = Files.list(dir)) {
                recordings.forEach(
                        path -> {
                            try {
                                Path exported = export(path).toPath();
                                Files.move(exported, camExportDir.resolve(exported.getFileName()));
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
            }

            exportedRecordings.add(camExportDir.toFile());
        }

        // Create a zip of all exported recordings
        File zipPath = Files.createTempFile("photonvision-recordings-export", ".zip").toFile();
        ZipUtil.packEntries(exportedRecordings.toArray(File[]::new), zipPath);

        return zipPath;
    }

    public static List<RecordingStrategy> getSupportedStrategies() {
        return List.of(RecordingStrategy.VIDEO);
    }
}
