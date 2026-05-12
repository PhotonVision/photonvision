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
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.opencv.CVMat;

/**
 * Replay-side counterpart to {@link org.photonvision.vision.pipeline.FrameRecorder}. Reads a
 * recording directory containing {@code recording.mp4} + {@code metadata.jsonl} and emits
 * {@link CapturedFrame}s with their original source-machine capture timestamps, so the rest of
 * the vision pipeline (and the NT publisher downstream of it) processes replayed frames
 * identically to live ones.
 *
 * <p>The mp4 is decoded by OpenCV's {@code VideoCapture} (which delegates to ffmpeg / Media
 * Foundation at the native layer). Each decoded frame is paired with the next sidecar entry,
 * giving us {@code (Mat, capture_ns)} tuples in the order they were captured. See
 * {@code FrameRecorder}'s class javadoc for the on-disk format and its invariants — this
 * reader's correctness depends on them.
 *
 * <h2>Timestamp contract</h2>
 *
 * <p>{@code capture_ns} is the <em>source</em> machine's {@code wpi::nt::Now} epoch at the
 * moment of capture, written verbatim by the recorder. We propagate it unchanged through
 * {@link CapturedFrame#captureTimestamp} into {@link org.photonvision.vision.frame.Frame#timestampNanos};
 * we do not substitute a replay-machine clock read. The value is therefore <em>opaque</em> to
 * everything downstream of the recorder — it represents source-machine time, and any consumer
 * that needs replay-machine wall-clock alignment is responsible for rebasing it. The two
 * Phase 1 consumers behave as follows:
 *
 * <ul>
 *   <li>{@code NTDataPublisher.accept} translates {@code capture_ns} through the Time Sync
 *       Server's offset (same as for a live source) before stamping NT updates.
 *   <li>An AKit-style wpilog NT4 capture on the replay machine records each NT event with
 *       its embedded {@code capture_ns} in the payload; AKit downstream reconstructs the
 *       timeline from those payload values regardless of when wpilog actually received them.
 * </ul>
 *
 * <h2>Calibration / FOV — manual import required</h2>
 *
 * <p>Recordings carry image data but not the camera intrinsics that captured them. This
 * provider constructs with {@link FrameStaticProperties} populated only from the mp4 header
 * (width, height) — FOV is 0 and {@code cameraCalibration} is {@code null}. Pipelines that
 * need intrinsics (AprilTag PnP, SolvePNP, coloured-shape distance estimation, anything that
 * reads {@code horizontalFocalLength} off the static properties) will produce nonsense
 * numbers until a calibration is associated with this source.
 *
 * <p><strong>Recommended workflow:</strong> after assigning a recording as a vision module
 * via the camera-matching UI, use the existing calibration upload UI
 * ({@code Cameras → Calibration → Import}) to attach the calibration that was active on the
 * source machine when the recording was captured. A future PR may add a {@code calibration_id}
 * field to the metadata sidecar (the writer's schema is already forward-compatible — see
 * {@link MetadataSidecarReader}) and auto-link on assignment; Phase 1 ships without that.
 *
 * <h2>Pacing</h2>
 *
 * <p>{@link #getInputMat()} <strong>blocks the vision thread</strong> so wall-clock playback
 * matches the recording's {@code capture_ns} deltas — anchors on the first frame, then targets
 * {@code firstMono + (captureNs - firstCapture)} for each subsequent frame. Uses
 * {@link System#nanoTime()} (monotonic; NTP / DST jumps don't disrupt playback) and
 * {@link java.util.concurrent.locks.LockSupport#parkNanos}. This keeps the UI's MJPEG stream
 * looking like a live source. AKit-style consumers don't strictly need pacing (they read
 * {@code capture_ns} from the payload regardless of receipt time) but the cost is negligible.
 * Pacing is target-based, not relative-sleep based, so a slow vision pipeline doesn't
 * accumulate sleep debt — if we're behind target, the next frame emits immediately.
 *
 * <h2>EOF policy</h2>
 *
 * <p>The shorter of the two streams ends replay. The writer guarantees
 * {@code len(jsonl) >= decoded_frame_count(mp4)} under any crash, so the mp4 normally
 * exhausts first; the jsonl-first path defends against externally truncated files. Once
 * exhausted, {@link #isConnected()} reports {@code false} and further
 * {@link #getInputMat()} calls short-circuit to empty {@link CVMat}s — which
 * {@link CpuImageProcessor#get()} handles cleanly without dropping or NPE.
 *
 * <h2>Pre-2183 recordings</h2>
 *
 * <p>A directory containing {@code recording.mp4} but no {@code metadata.jsonl} predates
 * PhotonVision's per-frame metadata sidecar. Without source-side capture timestamps the
 * replay timing contract cannot be honoured, so construction fails loudly with an
 * {@link IOException} that names the missing file — mirroring the writer-side refusal in
 * {@code FrameRecorder} when its sidecar cannot be opened.
 *
 * <h2>Recording-while-replaying</h2>
 *
 * <p>Unsupported on purpose. {@link #setRecording} throws
 * {@link UnsupportedOperationException}; {@link #getRecording()} is permanently {@code false}.
 * Re-encoding a recording into another recording would lose the original capture timestamps
 * and double the storage cost for no benefit.
 *
 * <h2>Beyond Phase 1</h2>
 *
 * <p>Phase 2 (AKit replay) is documented as "run wpilog NT4 capture against the replay
 * machine" — no PV-internal bridge code is needed. Phase 3 (headless batch replay,
 * faster-than-realtime) is a separate RFC about decoupling {@code CVPipeline} from
 * {@code VisionRunner} / NT / Javalin; the recording format itself is unchanged.
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

    // Pacing anchor — captured on the first emitted frame. nanoTime is monotonic so NTP jumps
    // and DST transitions don't disrupt playback. The pacing model is target-based, not
    // relative-sleep based, so a slow vision pipeline doesn't accumulate sleep debt.
    private boolean pacingAnchored = false;
    private long firstCaptureNs;
    private long firstMonoNs;

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

        long captureNs = entry.get().captureNs();
        pace(captureNs);

        emittedFrameCount++;
        return new CapturedFrame(mat, properties, captureNs);
    }

    /**
     * Block the vision thread so wall-clock spacing between emitted frames matches the
     * recording's {@code capture_ns} deltas. First call anchors; subsequent calls park until
     * {@code firstMono + (captureNs - firstCapture)} or return immediately if we're already
     * past target. Interrupts are honoured (flag preserved) so VisionRunner stops cleanly.
     */
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
            if (Thread.interrupted()) Thread.currentThread().interrupt();
        }
    }

    /**
     * Width / height / FOV / calibration of frames this provider will emit. Known at
     * construction time from the mp4 header; exposed so the surrounding VisionSource can
     * wire video-mode metadata into its settables without consuming a frame.
     */
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
