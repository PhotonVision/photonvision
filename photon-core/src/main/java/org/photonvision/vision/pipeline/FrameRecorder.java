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
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opencv.core.Mat;
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

public class FrameRecorder implements Releasable {
    private static final int QUEUE_CAPACITY = 30; // Buffer up to 30 frames
    private static final long MIN_DISK_SPACE_BYTES = 4L * 1024 * 1024 * 1024; // 4 GB

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

    // Per-frame metadata sidecar, one JSON object per line. Encoder-agnostic so it survives
    // any future change to the on-disk frame format (e.g. PNG -> H.264). Null if the file
    // couldn't be opened at construction.
    private BufferedWriter metadataWriter;

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

        // Open the metadata sidecar. Best-effort: if this fails we still record frames, just
        // without sidecar metadata (the filenames already contain seq + capture time as a
        // fallback for the snapshot strategy).
        try {
            this.metadataWriter =
                    java.nio.file.Files.newBufferedWriter(
                            outputPath.resolve("metadata.jsonl"),
                            java.nio.charset.StandardCharsets.UTF_8,
                            java.nio.file.StandardOpenOption.CREATE,
                            java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            logger.warn(
                    "Failed to open metadata sidecar at "
                            + outputPath
                            + "/metadata.jsonl: "
                            + e.getMessage());
            this.metadataWriter = null;
        }

        this.frameQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        this.recording = new AtomicBoolean(false);
        this.shutdown = new AtomicBoolean(false);

        // Start the writer thread
        switch (strat) {
            case SNAPSHOTS ->
                    this.writerThread = new Thread(this::snapshotLoop, "FrameRecorder-Snapshot");
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

        if (sequenceCounter % 100 == 0) {
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

    /** Worker thread that writes frames to video file */
    private void snapshotLoop() {
        while (!shutdown.get()) {
            try {
                RecordFrame frame = frameQueue.take();

                if (frame.isPoison) {
                    break;
                }
                // Filename is <seq>_<captureNs>. Seq-first so a lexicographic sort matches capture
                // order even if a camera ever reports out-of-order timestamps; captureNs preserves
                // the actual capture time for replay timing.
                String framePath =
                        String.format(
                                "%s/frame_%d_%d.png",
                                outputPath, frame.sequenceId, frame.captureTimestampNs);
                Imgcodecs.imwrite(framePath, frame.mat);

                writeMetadataLine(frame.sequenceId, frame.captureTimestampNs);

                // Release the cloned mat
                frame.mat.release();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Write one JSONL line: {"seq":N,"capture_ns":T}. Flushed per-line so a crash doesn't lose
     * the last few frames of metadata. Manually formatted (no Jackson) because the schema is
     * trivial and this is on the writer thread's hot path.
     */
    private void writeMetadataLine(long sequenceId, long captureTimestampNs) {
        if (metadataWriter == null) return;
        try {
            metadataWriter.write(
                    "{\"seq\":" + sequenceId + ",\"capture_ns\":" + captureTimestampNs + "}\n");
            metadataWriter.flush();
        } catch (java.io.IOException e) {
            logger.warn("Failed to write metadata sidecar line: " + e.getMessage());
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

        if (metadataWriter != null) {
            try {
                metadataWriter.close();
            } catch (java.io.IOException e) {
                logger.warn("Failed to close metadata sidecar: " + e.getMessage());
            }
            metadataWriter = null;
        }
    }

    /**
     * Export a recording at the given path.
     *
     * @param recording Path to recording directory
     * @return Path to exported recording
     */
    public static File export(Path recording) throws Exception {
        // Read the strategy used

        Path strategyFile = recording.resolve("strat");
        String strategy = Files.readString(strategyFile).trim();

        switch (strategy) {
            case "SNAPSHOTS":
                return exportSnapshots(recording);
            default:
                throw new IllegalArgumentException("Unsupported Recording Strategy: " + strategy);
        }
    }

    private static File exportSnapshots(Path recording) throws Exception {
        List<Snapshot> frames = new ArrayList<>();
        Pattern pattern = Pattern.compile("frame_(\\d+)_(\\d+)\\.png");

        File dir = recording.toFile();
        File[] files = dir.listFiles((d, name) -> name.matches("frame_\\d+_\\d+\\.png"));

        if (files != null) {
            for (File file : files) {
                Matcher matcher = pattern.matcher(file.getName());
                if (matcher.matches()) {
                    long sequenceId = Long.parseLong(matcher.group(1));
                    long captureNs = Long.parseLong(matcher.group(2));
                    frames.add(new Snapshot(file.getAbsolutePath(), sequenceId, captureNs));
                }
            }
        }

        Collections.sort(frames);

        if (frames.isEmpty()) {
            System.err.println("No frames found matching pattern frame_*_*.png");
            throw new IllegalStateException("No frames to export");
        }

        // Create concat file for FFmpeg
        Path concatFile = Files.createTempFile("ffmpeg_concat_", ".txt");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(concatFile.toFile()))) {
            for (int i = 0; i < frames.size(); i++) {
                Snapshot frame = frames.get(i);

                // Calculate duration based on time to next frame, using the capture timestamp
                // (recorded at the camera) rather than wall-clock at write time.
                double duration;
                if (i < frames.size() - 1) {
                    long timeDiff = frames.get(i + 1).captureNs - frame.captureNs;
                    duration = timeDiff / 1e9; // Convert ns to seconds
                } else {
                    // Last frame: use average frame duration or target fps
                    duration = 1.0 / 30.0; // Assume 30 FPS for last frame
                }

                writer.write("file '" + frame.filename + "'\n");
                writer.write("duration " + duration + "\n");
            }

            // FFmpeg concat requires the last file to be listed again without duration
            writer.write("file '" + frames.get(frames.size() - 1).filename + "'\n");

            File outputVideo = Files.createTempFile(recording.getFileName().toString(), ".mp4").toFile();

            // Build FFmpeg command for lossless encoding
            List<String> command = new ArrayList<>();
            command.add("ffmpeg");
            command.add("-f");
            command.add("concat");
            command.add("-safe");
            command.add("0");
            command.add("-i");
            command.add(concatFile.toString());
            command.add("-c:v");
            command.add("libx264");
            command.add("-preset");
            command.add("veryslow");
            command.add("-crf");
            command.add("0"); // Lossless
            command.add("-pix_fmt");
            command.add("yuv444p"); // Full chroma resolution
            command.add("-y");
            command.add(outputVideo.toString());

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                return outputVideo;
            } else {
                throw new RuntimeException("FFmpeg failed with exit code " + exitCode);
            }
        } finally {
            Files.deleteIfExists(concatFile);
        }
    }

    private static class Snapshot implements Comparable<Snapshot> {
        String filename;
        long sequenceId;
        long captureNs;

        Snapshot(String filename, long sequenceId, long captureNs) {
            this.filename = filename;
            this.sequenceId = sequenceId;
            this.captureNs = captureNs;
        }

        @Override
        public int compareTo(Snapshot other) {
            // Sort by sequence — capture order even if a camera's timestamps drift backward.
            return Long.compare(this.sequenceId, other.sequenceId);
        }
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
        return List.of(RecordingStrategy.SNAPSHOTS);
    }
}
