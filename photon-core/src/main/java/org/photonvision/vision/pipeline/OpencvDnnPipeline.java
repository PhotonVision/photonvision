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
import org.photonvision.vision.pipe.impl.OpencvDnnPipe.OpencvDnnPipeParams;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.TrackedTarget;
import org.photonvision.vision.target.TrackedTarget.TargetCalculationParameters;

public class OpencvDnnPipeline extends CVPipeline<CVPipelineResult, DnnPipelineSettings> {
    private final CalculateFPSPipe calculateFPSPipe = new CalculateFPSPipe();
    private final OpencvDnnPipe dnnPipe = new OpencvDnnPipe();

    private static final FrameThresholdType PROCESSING_TYPE = FrameThresholdType.NONE;

    public OpencvDnnPipeline() {
        super(PROCESSING_TYPE);
        settings = new DnnPipelineSettings();
    }

    public OpencvDnnPipeline(DnnPipelineSettings settings) {
        super(PROCESSING_TYPE);
        this.settings = settings;
    }

    @Override
    protected void setPipeParamsImpl() {
        dnnPipe.setParams(new OpencvDnnPipeParams(settings.modelPath, (float) settings.confidence));
    }

    @Override
    protected CVPipelineResult process(Frame input_frame, DnnPipelineSettings settings) {
        long sumPipeNanosElapsed = 0;

        CVPipeResult<List<NeuralNetworkPipeResult>> ret = dnnPipe.run(input_frame.colorImage);
        sumPipeNanosElapsed += ret.nanosElapsed;
        List<NeuralNetworkPipeResult> targetList;

        // TODO deal with RKNN stuf here
        targetList = ret.output;
        var names = dnnPipe.getClassNames();

        input_frame.colorImage.getMat().copyTo(input_frame.processedImage.getMat());

        List<TrackedTarget> targets = new ArrayList<>();

        // This belongs in a collect & draw pipe but I'm lazy
        int i = 0;
        for (var t : targetList) {
            // Imgproc.rectangle(
            //         input_frame.processedImage.getMat(), t.box.tl(), t.box.br(), new Scalar(0, 0, 255),
            // 2);

            // Let the draw pipeline deal with this all for us
            // var name = String.format("%d (%f)", i, t.confidence);
            // Imgproc.putText(
            //         input_frame.processedImage.getMat(),
            //         name,
            //         new Point(t.box.x + t.box.width / 2.0, t.box.y + t.box.height / 2.0),
            //         0,
            //         0.6,
            //         ColorHelper.colorToScalar(java.awt.Color.white),
            //         2);

            targets.add(
                    new TrackedTarget(
                            t,
                            new TargetCalculationParameters(
                                    false, null, null, null, null, frameStaticProperties)));

            i++;
        }

        var fpsResult = calculateFPSPipe.run(null);
        var fps = fpsResult.output;

        return new CVPipelineResult(sumPipeNanosElapsed, fps, targets, input_frame);
    }
}
