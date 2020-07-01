/*
 * Copyright (C) 2020 Photon Vision.
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

import org.photonvision.common.util.math.MathUtils;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.pipeline.result.CVPipelineResult;

import java.util.List;

public abstract class CVPipeline<R extends CVPipelineResult, S extends CVPipelineSettings> {
    protected S settings;

    protected abstract void setPipeParams(FrameStaticProperties frameStaticProperties, S settings);

    protected abstract R process(Frame frame, S settings);

    public S getSettings() {
        return settings;
    }

    // TODO (BANKS) ACTUALLY SET THE CAMERA RESOLUTION
    public R run(Frame frame) {
        long pipelineStartNanos = System.nanoTime();

        if (settings == null) {
            throw new RuntimeException("No settings provided for pipeline!");
        }
        setPipeParams(frame.frameStaticProperties, settings);

        if (frame.image.getMat().empty()) {
            return (R) new CVPipelineResult(0, List.of(), frame);
        }
        R result = process(frame, settings);

        result.setLatencyMillis(MathUtils.nanosToMillis(System.nanoTime() - pipelineStartNanos));

        return result;
    }
}
