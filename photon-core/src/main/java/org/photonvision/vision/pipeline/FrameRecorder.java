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
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.Releasable;
import org.photonvision.vision.processes.VisionModule;
import org.photonvision.vision.processes.VisionSourceManager;
import org.zeroturnaround.zip.ZipUtil;

public class FrameRecorder implements Releasable {
    private static final int QUEUE_CAPACITY = 30; // Buffer up to 30 frames

    private final BlockingQueue<RecordFrame> frameQueue;
    private final Thread writerThread;
    private final AtomicBoolean recording;
    private final AtomicBoolean shutdown;
    private int frameCounter = 0;

    public enum RecordingStrategy {
        SNAPSHOTS
    }

    public final RecordingStrategy strat;

    private Logger logger;

    private final Path outputPath;

    private static class RecordFrame {
        final Mat mat;
        final boolean isPoison;

        RecordFrame(Mat mat) {
            this.mat = mat;
            this.isPoison = false;
        }

        // Poison pill to signal shutdown
        private RecordFrame() {
            this.mat = null;
            this.isPoison = true;
        }

        static RecordFrame poison() {
            return new RecordFrame();
        }
    }

    public FrameRecorder(Path outputPath) {
        this.logger = new Logger(FrameRecorder.class, LogGroup.VisionModule);
        this.strat = HardwareManager.getInstance().getRecordingStrategy();

        double availableSpace = HardwareManager.getInstance().metricsManager.getDiskSpaceAvailable();

        // Check if we're under 4 GB of available space, if so exit
        if (availableSpace < 4 * 1024 * 1024) {
            logger.error(
                    "Low disk space available ("
                            + availableSpace / 1024
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
     * Record a frame. This is non-blocking - the frame is cloned and queued for writing. If the queue
     * is full, the oldest frame is dropped (this should rarely happen).
     *
     * @param cvmat The frame to record
     * @return true if frame was queued, false if recording is not active, queue is full, or we've run
     *     out of disk space.
     */
    public boolean recordFrame(CVMat cvmat) {
        if (!recording.get() || shutdown.get()) {
            return false;
        }

        if (frameCounter % 100 == 0) {
            double availableSpace = HardwareManager.getInstance().metricsManager.getDiskSpaceAvailable();

            // Check if we're under 4 GB of available space, if so stop recording
            if (availableSpace < 4 * 1024 * 1024) {
                logger.error(
                        "Low disk space available (" + availableSpace / 1024 + " MB). Stopping FrameRecorder.");
                stopRecording();
                return false;
            }
        }

        // Try to offer to queue; if full, drop frame (non-blocking)
        boolean added = frameQueue.offer(new RecordFrame(cvmat.getMat()));

        if (!added) {
            // Queue full, release the cloned mat
            cvmat.release();
        }

        frameCounter++;

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
                // Write frame to a file under the output path
                // For simplicity, we write each frame as an image file
                String framePath = String.format("%s/frame_%d.png", outputPath, System.currentTimeMillis());
                Imgcodecs.imwrite(framePath, frame.mat);

                // Release the cloned mat
                frame.mat.release();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
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
        frameQueue.offer(RecordFrame.poison());

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
        Pattern pattern = Pattern.compile("frame_(\\d+)\\.png");

        File dir = recording.toFile();
        File[] files = dir.listFiles((d, name) -> name.matches("frame_\\d+\\.png"));

        if (files != null) {
            for (File file : files) {
                Matcher matcher = pattern.matcher(file.getName());
                if (matcher.matches()) {
                    long timestamp = Long.parseLong(matcher.group(1));
                    frames.add(new Snapshot(file.getAbsolutePath(), timestamp));
                }
            }
        }

        Collections.sort(frames);

        if (frames.isEmpty()) {
            System.err.println("No frames found matching pattern frame_*.png");
            System.exit(1);
        }

        // Create concat file for FFmpeg
        Path concatFile = Files.createTempFile("ffmpeg_concat_", ".txt");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(concatFile.toFile()))) {
            for (int i = 0; i < frames.size(); i++) {
                Snapshot frame = frames.get(i);

                // Calculate duration based on time to next frame
                double duration;
                if (i < frames.size() - 1) {
                    long timeDiff = frames.get(i + 1).timestamp - frame.timestamp;
                    duration = timeDiff / 1000.0; // Convert ms to seconds
                } else {
                    // Last frame: use average frame duration or target fps
                    duration = 1.0 / 30.0; // Assume 30 FPS for last frame
                }

                writer.write("file '" + frame.filename + "'\n");
                writer.write("duration " + duration + "\n");
            }

            // FFmpeg concat requires the last file to be listed again without duration
            writer.write("file '" + frames.get(frames.size() - 1).filename + "'\n");
        }

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

        // Cleanup
        Files.deleteIfExists(concatFile);

        if (exitCode == 0) {
            return outputVideo;
        } else {
            throw new RuntimeException("FFmpeg failed with exit code " + exitCode);
        }
    }

    private static class Snapshot implements Comparable<Snapshot> {
        String filename;
        long timestamp;

        Snapshot(String filename, long timestamp) {
            this.filename = filename;
            this.timestamp = timestamp;
        }

        @Override
        public int compareTo(Snapshot other) {
            return Long.compare(this.timestamp, other.timestamp);
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
                                                .map(module -> module.getCameraConfiguration().nickname)
                                                .toString()
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

            Files.list(camExportDir)
                    .forEach(
                            path -> {
                                try {
                                    Files.move(export(path).toPath(), camExportDir);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            });

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
