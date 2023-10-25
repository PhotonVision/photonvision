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

import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.pipeline.result.CVPipelineResult;

public abstract class CVPipeline<R extends CVPipelineResult, S extends CVPipelineSettings> {
    protected S settings;
    protected FrameStaticProperties frameStaticProperties;
    protected QuirkyCamera cameraQuirks;

    private final FrameThresholdType thresholdType;

    public CVPipeline(FrameThresholdType thresholdType) {
        this.thresholdType = thresholdType;
    }

    public FrameThresholdType getThresholdType() {
        return thresholdType;
    }

    protected void setPipeParams(
            FrameStaticProperties frameStaticProperties, S settings, QuirkyCamera cameraQuirks) {
        this.settings = settings;
        this.frameStaticProperties = frameStaticProperties;
        this.cameraQuirks = cameraQuirks;

        setPipeParamsImpl();
    }

    protected abstract void setPipeParamsImpl();

    protected abstract R process(Frame frame, S settings);

    public S getSettings() {
        return settings;
    }

    public void setSettings(S s) {
        this.settings = s;
    }

    public R run(Frame frame, QuirkyCamera cameraQuirks) {
        if (settings == null) {
            throw new RuntimeException("No settings provided for pipeline!");
        }
        setPipeParams(frame.frameStaticProperties, settings, cameraQuirks);

        // if (frame.image.getMat().empty()) {
        //     //noinspection unchecked
        //     return (R) new CVPipelineResult(0, 0, List.of(), frame);
        // }
        R result = process(frame, settings);

        result.setImageCaptureTimestampNanos(frame.timestampNanos);

        return result;
    }
}
