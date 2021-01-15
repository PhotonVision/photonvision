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
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.opencv.Releasable;
import org.photonvision.vision.target.TrackedTarget;

public class CVPipelineResult implements Releasable {
    private double latencyMillis;
    public final double processingMillis;
    public final double fps;
    public final List<TrackedTarget> targets;
    public final Frame outputFrame;
    public final Frame inputFrame;

    public CVPipelineResult(
            double processingMillis,
            double fps,
            List<TrackedTarget> targets,
            Frame outputFrame,
            Frame inputFrame) {
        this.processingMillis = processingMillis;
        this.fps = fps;
        this.targets = targets != null ? targets : Collections.emptyList();

        this.outputFrame = outputFrame;
        this.inputFrame = inputFrame;
    }

    public CVPipelineResult(
            double processingMillis, double fps, List<TrackedTarget> targets, Frame outputFrame) {
        this(processingMillis, fps, targets, outputFrame, null);
    }

    public boolean hasTargets() {
        return !targets.isEmpty();
    }

    public void release() {
        for (TrackedTarget tt : targets) {
            tt.release();
        }
        outputFrame.release();
        if (inputFrame != null) inputFrame.release();
    }

    public double getLatencyMillis() {
        return latencyMillis;
    }

    public void setLatencyMillis(double latencyMillis) {
        this.latencyMillis = latencyMillis;
    }
}
