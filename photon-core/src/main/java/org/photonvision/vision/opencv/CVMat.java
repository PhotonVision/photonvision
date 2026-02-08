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

package org.photonvision.vision.opencv;

import edu.wpi.first.util.RawFrame;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.opencv.core.Mat;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class CVMat implements Releasable {
    private static final Logger logger = new Logger(CVMat.class, LogGroup.General);
    private static final AtomicInteger matIdCounter = new AtomicInteger(0);

    // All mats that have not yet been released(). these may still need to be GCed
    private static final Set<MatTracker> allMats = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final ReferenceQueue<CVMat> refQueue = new ReferenceQueue<>();

    private static boolean shouldPrint;

    private Mat mat;
    private RawFrame backingFrame;
    public final int matId;
    private final MatTracker tracker;
    private volatile boolean released = false;

    /**
     * Track a single CVMat instance using a PhantomReference
     */
    private static class MatTracker extends PhantomReference<CVMat> {
        final int id;
        final long nativePtr;
        final String allocTrace;
        volatile boolean explicitlyReleased = false;

        MatTracker(CVMat cvmat, int id, ReferenceQueue<CVMat> queue) {
            super(cvmat, queue);
            this.id = id;
            this.nativePtr = cvmat.mat.nativeObj;
            this.allocTrace = shouldPrint ? getStackTrace() : null;
        }

        private static String getStackTrace() {
            var trace = Thread.currentThread().getStackTrace();
            final int SKIP = 4; // Skip getStackTrace, <init>, CVMat.<init>, caller
            var sb = new StringBuilder();
            for (int i = SKIP; i < Math.min(trace.length, SKIP + 10); i++) {
                sb.append("\n\t").append(trace[i]);
            }
            return sb.toString();
        }
    }

    public CVMat() {
        this(new Mat());
    }

    public CVMat(Mat mat) {
        this(mat, null);
    }

    public CVMat(Mat mat, RawFrame frame) {
        this.mat = mat;
        this.backingFrame = frame;
        this.matId = matIdCounter.incrementAndGet();
        this.tracker = new MatTracker(this, matId, refQueue);

        allMats.add(tracker);

        if (shouldPrint) {
            logger.debug(
                    "CVMat"
                            + matId
                            + " allocated - count: "
                            + allMats.size()
                            + (tracker.allocTrace != null ? tracker.allocTrace : ""));
        }
    }

    public void copyFrom(CVMat srcMat) {
        copyFrom(srcMat.getMat());
    }

    public void copyFrom(Mat srcMat) {
        srcMat.copyTo(mat);
    }

    @Override
    public void release() {
        synchronized (this) {
            if (released) {
                if (shouldPrint) {
                    logger.error("CVMat" + matId + " already released (ignored)");
                }
                return;
            }
            released = true;
        }

        tracker.explicitlyReleased = true;

        // Free RawFrames exactly ONCE
        if (backingFrame != null) {
            try {
                backingFrame.close();
                backingFrame = null;
            } catch (Exception e) {
                logger.error("Error closing RawFrame for CVMat" + matId, e);
            }
        }

        try {
            if (mat != null) {
                mat.release();
                mat = null;
            } else {
                logger.error("Mat was already null, this is a no-op");
            }
        } catch (Exception e) {
            logger.error("Error releasing Mat for CVMat" + matId, e);
        }

        // write down it's freed
        allMats.remove(tracker);

        if (shouldPrint) {
            logger.debug("CVMat" + matId + " released - count: " + allMats.size());
        }
    }

    public Mat getMat() {
        if (released) {
            throw new IllegalStateException("CVMat" + matId + " has been released!");
        }
        return mat;
    }

    public boolean isReleased() {
        return released;
    }

    @Override
    public String toString() {
        return "CVMat [mat="
                + mat
                + ", backingFrame="
                + backingFrame
                + ", matId="
                + matId
                + ", tracker="
                + tracker
                + ", released="
                + released
                + "]";
    }

    public static int getMatCount() {
        return allMats.size();
    }

    public static void enablePrint(boolean enabled) {
        shouldPrint = enabled;
    }

    // todo move to somewhere else
    static {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    MatTracker ref = (MatTracker) refQueue.remove();

                    // Check if it was released before GC
                    if (!ref.explicitlyReleased) {
                        // This is a leak - remove from tracking and warn
                        allMats.remove(ref);

                        logger.warn("CVMat" + ref.id + " was GC'd without release()! " +
                                "Native memory leaked. Ptr: 0x" +
                                Long.toHexString(ref.nativePtr));
                        if (ref.allocTrace != null) {
                            logger.warn("Allocated at:" + ref.allocTrace);
                        }
                    }
                    // If explicitlyReleased == true, it was already removed from allMats
                    // in release(), so nothing to do here

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "CVMat-Cleanup");
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }

    // Paranoia
    @Override
    @SuppressWarnings("deprecation")
    protected void finalize() throws Throwable {
        try {
            if (!released) {
                logger.error("CVMat" + matId + " finalized without release()! Leaking native memory.");
                // Don't call release() here - finalization order is unpredictable
                // and backingFrame might already be finalized
            }
        } finally {
            super.finalize();
        }
    }
}
