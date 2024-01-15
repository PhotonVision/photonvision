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

import java.util.ArrayList;
import java.util.List;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.pipe.CVPipe.CVPipeResult;
import org.photonvision.vision.pipe.impl.*;
import org.photonvision.vision.pipe.impl.RknnDetectionPipe.RknnDetectionPipeParams;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.TrackedTarget;
import org.photonvision.vision.target.TrackedTarget.TargetCalculationParameters;

public class ObjectDetectionPipeline
        extends CVPipeline<CVPipelineResult, ObjectDetectionPipelineSettings> {
    private final CalculateFPSPipe calculateFPSPipe = new CalculateFPSPipe();
    private final RknnDetectionPipe rknnPipe = new RknnDetectionPipe();

    private static final FrameThresholdType PROCESSING_TYPE = FrameThresholdType.NONE;

    public ObjectDetectionPipeline() {
        super(PROCESSING_TYPE);
        settings = new ObjectDetectionPipelineSettings();
    }

    public ObjectDetectionPipeline(ObjectDetectionPipelineSettings settings) {
        super(PROCESSING_TYPE);
        this.settings = settings;
    }

    @Override
    protected void setPipeParamsImpl() {
        // this needs to be based off of the current backend selected!!
        var params = new RknnDetectionPipeParams();
        params.confidence = settings.confidence;
        params.nms = settings.nms;
        rknnPipe.setParams(params);
    }

    @Override
    protected CVPipelineResult process(Frame input_frame, ObjectDetectionPipelineSettings settings) {
        long sumPipeNanosElapsed = 0;

        // ***************** change based on backend ***********************

        CVPipeResult<List<NeuralNetworkPipeResult>> ret = rknnPipe.run(input_frame.colorImage);
        sumPipeNanosElapsed += ret.nanosElapsed;
        List<NeuralNetworkPipeResult> targetList;

        targetList = ret.output;
        var names = rknnPipe.getClassNames();

        input_frame.colorImage.getMat().copyTo(input_frame.processedImage.getMat());

        // ***************** change based on backend ***********************

        List<TrackedTarget> targets = new ArrayList<>();

        for (var t : targetList) {
            targets.add(
                    new TrackedTarget(
                            t,
                            new TargetCalculationParameters(
                                    false, null, null, null, null, frameStaticProperties)));
        }

        var fpsResult = calculateFPSPipe.run(null);
        var fps = fpsResult.output;

        return new CVPipelineResult(sumPipeNanosElapsed, fps, targets, input_frame, names);
    }

    @Override
    public void release() {
        rknnPipe.release();
    }
}
