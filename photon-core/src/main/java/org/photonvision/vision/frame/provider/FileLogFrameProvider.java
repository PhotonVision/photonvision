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
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.opencv.CVMat;

/**
 * Replay-side counterpart to {@code FrameRecorder}. Reads a recording directory containing
 * {@code recording.mp4} + {@code metadata.jsonl} and emits {@link CapturedFrame}s with their
 * original source-machine capture timestamps.
 *
 * <p>The mp4 is decoded by OpenCV's {@code VideoCapture} (which delegates to ffmpeg/Media
 * Foundation at the native layer). Each decoded frame is paired with the next sidecar entry,
 * giving us {@code (Mat, capture_ns)} tuples in the order they were captured.
 *
 * <p><strong>Timestamp contract:</strong> {@code capture_ns} is the <em>source</em> machine's
 * {@code wpi::nt::Now} epoch as written by the recorder. We propagate it verbatim through
 * {@code Frame.timestampNanos}; we do not substitute a replay-machine clock read. Downstream
 * (the NT publisher, AKit log capture) treats it as opaque time and rebases if it needs to.
 *
 * <p><strong>EOF policy:</strong> the shorter of the two streams ends replay. The writer
 * guarantees {@code len(jsonl) >= decoded_frame_count(mp4)} under any crash, so in practice the
 * mp4 always exhausts first. The jsonl-first path defends against externally truncated files.
 *
 * <p><strong>Pre-2183 recordings:</strong> a directory containing {@code recording.mp4} but no
 * {@code metadata.jsonl} predates this PR's per-frame metadata. Without source-side capture
 * timestamps we cannot guarantee the replay timing contract, so construction fails loudly —
 * mirrors the writer-side refusal in {@code FrameRecorder} when the sidecar cannot be opened.
 *
 * <p><strong>What this commit does NOT do</strong> (deliberately deferred to later commits):
 *
 * <ul>
 *   <li>Pacing — frames are returned as fast as {@code VideoCapture.read()} produces them.
 *       A {@code PacingStrategy} seam arrives in a follow-up commit.
 *   <li>FOV / calibration — placeholder {@code FrameStaticProperties} with FOV=0.0 and no
 *       calibration. Users import calibration post-assignment via the existing upload UI; a
 *       later commit documents this.
 *   <li>Recording-while-replaying — unsupported on purpose; {@link #setRecording} throws.
 * </ul>
 */
public class FileLogFrameProvider extends CpuImageProcessor {
    private final Path recordingDir;
    private final VideoCapture videoCapture;
    private final MetadataSidecarReader sidecarReader;
    private final FrameStaticProperties properties;
    private final Logger logger;

    // Once true, all subsequent getInputMat() calls short-circuit to empty frames and
    // isConnected() returns false. Single-threaded use is the norm (VisionRunner calls
    // getInputMat from one thread) but AtomicBoolean documents the intent clearly.
    private final AtomicBoolean exhausted = new AtomicBoolean(false);

    private long emittedFrameCount = 0;

    /**
     * @param recordingDir directory containing {@code recording.mp4} and {@code metadata.jsonl},
     *     as written by {@code FrameRecorder}.
     * @throws IOException if either file is missing, the mp4 cannot be opened, or the sidecar
     *     cannot be read.
     */
    public FileLogFrameProvider(Path recordingDir) throws IOException {
        this.recordingDir = recordingDir;
        Path videoPath = recordingDir.resolve("recording.mp4");
        Path metadataPath = recordingDir.resolve("metadata.jsonl");

        if (!Files.isRegularFile(videoPath)) {
            throw new IOException(
                    "Recording directory " + recordingDir + " is missing recording.mp4");
        }
        if (!Files.isRegularFile(metadataPath)) {
            throw new IOException(
                    "Recording at "
                            + recordingDir
                            + " is missing metadata.jsonl. Recordings made before the per-frame"
                            + " metadata PR cannot be replayed — their source-side capture timestamps"
                            + " are unrecoverable.");
        }

        this.videoCapture = new VideoCapture(videoPath.toString());
        if (!this.videoCapture.isOpened()) {
            throw new IOException(
                    "OpenCV VideoCapture could not open "
                            + videoPath
                            + " — verify the file is readable and the OpenCV build includes a"
                            + " backend (ffmpeg / Media Foundation) capable of decoding it.");
        }

        int width = (int) this.videoCapture.get(Videoio.CAP_PROP_FRAME_WIDTH);
        int height = (int) this.videoCapture.get(Videoio.CAP_PROP_FRAME_HEIGHT);
        if (width <= 0 || height <= 0) {
            this.videoCapture.release();
            throw new IOException(
                    "Recording " + videoPath + " reports invalid dimensions " + width + "x" + height);
        }

        // FOV unknown until the user imports calibration for this source. The pipelines tolerate
        // FOV=0 / null calibration; documented as a Phase 1 caveat in a follow-up commit.
        this.properties = new FrameStaticProperties(width, height, 0.0, null);

        try {
            this.sidecarReader = new MetadataSidecarReader(metadataPath);
        } catch (IOException e) {
            // Don't leak the native VideoCapture handle if the sidecar open fails.
            this.videoCapture.release();
            throw e;
        }

        this.logger =
                new Logger(
                        FileLogFrameProvider.class,
                        recordingDir.getFileName().toString(),
                        LogGroup.Camera);
        this.logger.info("Opened replay source " + recordingDir + " (" + width + "x" + height + ")");
    }

    @Override
    public CapturedFrame getInputMat() {
        // Mirror USBFrameProvider's defensive fire-once pattern: if nobody called isConnected()
        // between construction and the first getInputMat (the VisionRunner startup loop normally
        // does, but tests and future callers may not), make sure onCameraConnected runs so
        // hasConnected() reports the truth. For a file source we know we're alive the moment
        // construction succeeds — the file is right there.
        if (!cameraPropertiesCached) {
            onCameraConnected();
        }

        if (exhausted.get()) {
            return new CapturedFrame(new CVMat(), properties, 0L);
        }

        // Decode the next frame. VideoCapture.read returns false at EOF or on decode error;
        // we treat both the same — the stream is over.
        var mat = new CVMat();
        boolean haveFrame = videoCapture.read(mat.getMat());
        if (!haveFrame || mat.getMat().empty()) {
            mat.release();
            markExhausted("mp4 exhausted after " + emittedFrameCount + " frames");
            return new CapturedFrame(new CVMat(), properties, 0L);
        }

        Optional<MetadataSidecarReader.Entry> entry;
        try {
            entry = sidecarReader.readNext();
        } catch (IOException e) {
            mat.release();
            logger.error(
                    "metadata.jsonl read failed at frame "
                            + emittedFrameCount
                            + "; stopping replay: "
                            + e.getMessage());
            markExhausted("metadata read failure");
            return new CapturedFrame(new CVMat(), properties, 0L);
        }

        if (entry.isEmpty()) {
            // Should not happen given the writer's len(jsonl) >= mp4 invariant; defend anyway.
            mat.release();
            markExhausted(
                    "metadata.jsonl exhausted before mp4 at frame " + emittedFrameCount);
            return new CapturedFrame(new CVMat(), properties, 0L);
        }

        emittedFrameCount++;
        return new CapturedFrame(mat, properties, entry.get().captureNs());
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
        videoCapture.release();
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
