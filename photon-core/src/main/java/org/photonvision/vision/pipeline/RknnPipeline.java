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
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect2d;
import org.opencv.core.Point;
import org.opencv.core.Rect2d;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;
import org.photonvision.common.util.ColorHelper;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.pipe.CVPipe.CVPipeResult;
import org.photonvision.vision.pipe.impl.*;
import org.photonvision.vision.pipe.impl.RknnDetectionPipe.RknnDetectionPipeParams;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.TrackedTarget;
import org.photonvision.vision.target.TrackedTarget.TargetCalculationParameters;

public class RknnPipeline extends CVPipeline<CVPipelineResult, RknnPipelineSettings> {
    private final CalculateFPSPipe calculateFPSPipe = new CalculateFPSPipe();
    private final RknnDetectionPipe rknnPipe = new RknnDetectionPipe();

    private static final FrameThresholdType PROCESSING_TYPE = FrameThresholdType.NONE;

    public RknnPipeline() {
        super(PROCESSING_TYPE);
        settings = new RknnPipelineSettings();
    }

    public RknnPipeline(RknnPipelineSettings settings) {
        super(PROCESSING_TYPE);
        this.settings = settings;
    }

    @Override
    protected void setPipeParamsImpl() {
        var params = new RknnDetectionPipeParams();
        params.confidence = settings.confidence;
        rknnPipe.setParams(params);
    }

    @Override
    protected CVPipelineResult process(Frame input_frame, RknnPipelineSettings settings) {
        long sumPipeNanosElapsed = 0;

        CVPipeResult<List<NeuralNetworkPipeResult>> ret = rknnPipe.run(input_frame.colorImage);
        sumPipeNanosElapsed += ret.nanosElapsed;
        List<NeuralNetworkPipeResult> targetList;

        targetList = ret.output;
        var names = rknnPipe.getParams().getClassNames();

        input_frame.colorImage.getMat().copyTo(input_frame.processedImage.getMat());

        List<TrackedTarget> targets = new ArrayList<>();

        // This belongs in a collect & draw pipe but I'm lazy
        for (var t : targetList) {
            //Imgproc.rectangle(input_frame.processedImage.getMat(), t.box.tl(), t.box.br(), new Scalar(0, 0, 255), 2);

            var name = String.format("%s (%.1f%%)", names.get(t.classIdx), t.confidence*100);

            Imgproc.putText(
                    input_frame.processedImage.getMat(),
                    name,
                    new Point(t.box.x + t.box.width / 2.5, t.box.y + t.box.height / 2.0),
                    0,
                    1,
                    ColorHelper.colorToScalar(java.awt.Color.green),
                    2);

            targets.add(
                    new TrackedTarget(
                            t,
                            new TargetCalculationParameters(
                                    false, null, null, null, null, frameStaticProperties)));
        }

        var fpsResult = calculateFPSPipe.run(null);
        var fps = fpsResult.output;

        return new CVPipelineResult(sumPipeNanosElapsed, fps, targets, input_frame);
    }
}