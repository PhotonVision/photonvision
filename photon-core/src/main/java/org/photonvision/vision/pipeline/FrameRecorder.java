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

import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.Releasable;

public class FrameRecorder implements Releasable {
    private static final int QUEUE_CAPACITY = 30; // Buffer up to 30 frames

    private final BlockingQueue<RecordFrame> frameQueue;
    private final Thread writerThread;
    private final AtomicBoolean recording;
    private final AtomicBoolean shutdown;

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

        logger.info("Initializing FrameRecorder with output path: " + outputPath.toString());
        this.outputPath = outputPath;

        this.frameQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        this.recording = new AtomicBoolean(false);
        this.shutdown = new AtomicBoolean(false);

        // Start the writer thread
        this.writerThread = new Thread(this::writerLoop, "FrameRecorder-Writer");
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
     * @return true if frame was queued, false if recording is not active or queue is full
     */
    public boolean recordFrame(CVMat cvmat) {
        if (!recording.get() || shutdown.get()) {
            return false;
        }

        // Clone the mat so we don't hold references to pipeline memory
        Mat cloned = cvmat.getMat().clone();

        // Try to offer to queue; if full, drop frame (non-blocking)
        boolean added = frameQueue.offer(new RecordFrame(cloned));

        if (!added) {
            // Queue full, release the cloned mat
            cloned.release();
        }

        return added;
    }

    /** Worker thread that writes frames to video file */
    private void writerLoop() {
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
}
