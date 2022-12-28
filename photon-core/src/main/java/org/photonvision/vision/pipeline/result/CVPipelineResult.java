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

package org.photonvision.vision.pipeline.result;

import java.util.Collections;
import java.util.List;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.opencv.Releasable;
import org.photonvision.vision.target.TrackedTarget;

public class CVPipelineResult implements Releasable {
    private long imageCaptureTimestampNanos;
    public final double processingNanos;
    public final double fps;
    public final List<TrackedTarget> targets;
    public final Frame inputAndOutputFrame;

    public CVPipelineResult(
            double processingNanos, double fps, List<TrackedTarget> targets, Frame inputFrame) {
        this.processingNanos = processingNanos;
        this.fps = fps;
        this.targets = targets != null ? targets : Collections.emptyList();

        this.inputAndOutputFrame = inputFrame;
    }

    public CVPipelineResult(double processingNanos, double fps, List<TrackedTarget> targets) {
        this(processingNanos, fps, targets, null);
    }

    public boolean hasTargets() {
        return !targets.isEmpty();
    }

    public void release() {
        for (TrackedTarget tt : targets) {
            tt.release();
        }
        if (inputAndOutputFrame != null) inputAndOutputFrame.release();
    }

    /**
     * Get the latency between now (wpi::Now) and the time at which the image was captured. FOOTGUN:
     * the latency is relative to the time at which this method is called. Waiting to call this method
     * will change the latency this method returns.
     */
    @Deprecated
    public double getLatencyMillis() {
        var now = MathUtils.wpiNanoTime();
        return MathUtils.nanosToMillis(now - imageCaptureTimestampNanos);
    }

    public double getProcessingMillis() {
        return MathUtils.nanosToMillis(processingNanos);
    }

    public long getImageCaptureTimestampNanos() {
        return imageCaptureTimestampNanos;
    }

    public void setImageCaptureTimestampNanos(long imageCaptureTimestampNanos) {
        this.imageCaptureTimestampNanos = imageCaptureTimestampNanos;
    }
}
