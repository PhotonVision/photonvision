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
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.opencv.core.Mat;
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
 * Writes an H.264 mp4 plus a metadata.jsonl sidecar of per-frame (seq, capture_ns) pairs.
 *
 * <p>{@code capture_ns} is the source machine's {@code wpi::nt::Now} epoch at capture, recorded
 * verbatim. Replay readers must propagate it through {@code Frame.timestampNanos} unchanged —
 * the replay machine's clock is irrelevant; downstream consumers (NT publisher, AKit) treat it
 * as opaque time and rebase if they need to.
 *
 * <p>Metadata is flushed before its paired frame, so under any crash
 * {@code len(metadata.jsonl) >= decoded_frame_count(recording.mp4)}. Readers truncate jsonl to
 * the mp4's decoded count, and ignore unknown JSON fields so the schema can grow (exposure,
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

    // FFmpeg subprocess that encodes raw BGR frames to H.264. Started lazily on the first
    // recordFrame call so we have the frame dimensions to pass to ffmpeg's -s argument.
    private Process ffmpegProcess;
    private OutputStream ffmpegStdin;

    // Reused per-frame byte buffer for the JNI Mat -> byte[] copy. Allocated once per
    // recording (assuming constant resolution).
    private byte[] frameBuffer;

    // Per-frame metadata sidecar, one JSON object per line. Encoder-agnostic so it survives
    // the H.264 switch — filenames no longer carry per-frame info, sidecar is the only
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
        this.logger = new Logger(FrameRecorder.class, LogGroup.VisionModule);
        this.strat = HardwareManager.getInstance().getRecordingStrategy();

        double availableSpace = SystemMonitor.getInstance().getUsableDiskSpace();

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
        this.outputVideoPath = outputPath.resolve("recording.mp4");

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
        // produce a recording.mp4 missing the seq + capture_ns needed to feed frames back
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

    /** Worker thread: pipes raw BGR frames into the ffmpeg subprocess for live H.264 encoding. */
    private void videoLoop() {
        while (!shutdown.get()) {
            try {
                RecordFrame frame = frameQueue.take();

                if (frame.isPoison) {
                    break;
                }

                if (ffmpegProcess == null) {
                    if (!startFfmpeg(frame.mat.cols(), frame.mat.rows())) {
                        frame.mat.release();
                        // Encoder couldn't start; stop recording so we don't busy-drain the queue.
                        stopRecording();
                        continue;
                    }
                }

                // Metadata before frame: keeps len(jsonl) >= mp4 frame count under any crash,
                // so replay readers truncate jsonl to the mp4's decoded count — never the
                // other way around. If metadata write fails, skip the frame to preserve the
                // invariant (writeMetadataLine stops recording on failure).
                if (writeMetadataLine(frame.sequenceId, frame.captureTimestampNs)) {
                    writeRawFrame(frame.mat);
                }

                frame.mat.release();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Spawn ffmpeg with stdin piped: raw BGR frames in, H.264 mp4 out.
     *
     * <p>Encoder is libx264 with preset=ultrafast + tune=zerolatency to keep CPU load low enough
     * for 30 fps on a Pi-class device. crf 23 is FFmpeg's default quality target.
     *
     * @return true on success, false if the subprocess couldn't be started (ffmpeg not on PATH,
     *     etc.) — caller should stop recording.
     */
    private boolean startFfmpeg(int width, int height) {
        ProcessBuilder pb =
                new ProcessBuilder(
                        "ffmpeg",
                        "-hide_banner",
                        "-loglevel",
                        "warning",
                        "-y",
                        "-f",
                        "rawvideo",
                        "-pix_fmt",
                        "bgr24",
                        "-s",
                        width + "x" + height,
                        "-r",
                        "30",
                        "-i",
                        "pipe:0",
                        "-c:v",
                        "libx264",
                        "-preset",
                        "ultrafast",
                        "-tune",
                        "zerolatency",
                        "-crf",
                        "23",
                        "-pix_fmt",
                        "yuv420p",
                        outputVideoPath.toString());
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        try {
            ffmpegProcess = pb.start();
            ffmpegStdin = ffmpegProcess.getOutputStream();
            logger.info(
                    "Started ffmpeg subprocess for " + width + "x" + height + " -> " + outputVideoPath);
            return true;
        } catch (IOException e) {
            logger.error(
                    "Failed to start ffmpeg subprocess (is ffmpeg installed and on PATH?): "
                            + e.getMessage());
            ffmpegProcess = null;
            ffmpegStdin = null;
            return false;
        }
    }

    /**
     * Copy the BGR pixel bytes out of the Mat and write them to ffmpeg's stdin.
     *
     * @return true on success, false if ffmpeg's pipe is broken (subprocess died) — caller should
     *     stop recording.
     */
    private boolean writeRawFrame(Mat mat) {
        int needed = (int) (mat.total() * mat.elemSize());
        if (frameBuffer == null || frameBuffer.length < needed) {
            frameBuffer = new byte[needed];
        }
        mat.get(0, 0, frameBuffer);
        try {
            ffmpegStdin.write(frameBuffer, 0, needed);
            return true;
        } catch (IOException e) {
            logger.error(
                    "ffmpeg pipe broken (subprocess died?): "
                            + e.getMessage()
                            + "; stopping recording.");
            stopRecording();
            return false;
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

        // Close ffmpeg's stdin to signal EOF, wait for it to finalize the .mp4 (write the
        // moov atom and exit). If it doesn't exit promptly something's wrong; force-kill.
        if (ffmpegStdin != null) {
            try {
                ffmpegStdin.close();
            } catch (IOException e) {
                logger.warn("Failed to close ffmpeg stdin: " + e.getMessage());
            }
            ffmpegStdin = null;
        }
        if (ffmpegProcess != null) {
            try {
                if (!ffmpegProcess.waitFor(5, TimeUnit.SECONDS)) {
                    logger.warn("ffmpeg did not exit within 5s; force-killing.");
                    ffmpegProcess.destroyForcibly();
                } else if (ffmpegProcess.exitValue() != 0) {
                    logger.warn("ffmpeg exited with code " + ffmpegProcess.exitValue());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                ffmpegProcess.destroyForcibly();
            }
            ffmpegProcess = null;
        }
    }

    /**
     * Export a recording.
     *
     * <p>Returns a temp copy of {@code <recording>/recording.mp4} so callers (single-recording
     * export, exportCamera, exportAll) can safely move/zip without disturbing the original.
     *
     * @param recording Path to recording directory
     * @return Path to a unique temp file with the exported .mp4
     */
    public static File export(Path recording) throws Exception {
        Path mp4 = recording.resolve("recording.mp4");
        if (!Files.exists(mp4)) {
            throw new IllegalStateException("No recording.mp4 found in " + recording);
        }
        // Prefix with the recording's dir name so exportCamera's ZIP entries are unique.
        File copy =
                Files.createTempFile(recording.getFileName().toString() + "_", ".mp4").toFile();
        Files.copy(mp4, copy.toPath(), StandardCopyOption.REPLACE_EXISTING);
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
