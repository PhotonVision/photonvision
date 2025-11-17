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

import org.opencv.core.Mat;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipe.impl.CalculateFPSPipe;
import org.photonvision.vision.pipe.impl.FocusPipe;
import org.photonvision.vision.pipe.impl.ResizeImagePipe;
import org.photonvision.vision.pipeline.result.FocusPipelineResult;

public class FocusPipeline extends CVPipeline<FocusPipelineResult, FocusPipelineSettings> {
    private final FocusPipe focusPipe = new FocusPipe();
    private final CalculateFPSPipe calculateFPSPipe = new CalculateFPSPipe();
    private final ResizeImagePipe resizeImagePipe = new ResizeImagePipe();

    private static final FrameThresholdType PROCESSING_TYPE = FrameThresholdType.NONE;

    public FocusPipeline() {
        super(PROCESSING_TYPE);
        settings = new FocusPipelineSettings();
    }

    public FocusPipeline(FocusPipelineSettings settings) {
        super(PROCESSING_TYPE);
        this.settings = settings;
    }

    @Override
    protected void setPipeParamsImpl() {
        resizeImagePipe.setParams(
                new ResizeImagePipe.ResizeImageParams(settings.streamingFrameDivisor));
    }

    @Override
    public FocusPipelineResult process(Frame frame, FocusPipelineSettings settings) {
        long totalNanos = 0;

        var inputMat = frame.colorImage.getMat();
        boolean emptyIn = inputMat.empty();
        Mat displayMat = new Mat();
        double variance = 0.0;

        if (!emptyIn) {
            totalNanos += resizeImagePipe.run(inputMat).nanosElapsed;

            var focusResult = focusPipe.run(inputMat);
            totalNanos += focusResult.nanosElapsed;
            variance = focusResult.output.variance;
            displayMat = focusResult.output.frame;
        }

        var fpsResult = calculateFPSPipe.run(null);
        var fps = fpsResult.output;

        var processedCVMat = new CVMat(displayMat);

        return new FocusPipelineResult(
                frame.sequenceID,
                MathUtils.nanosToMillis(totalNanos),
                fps,
                new Frame(
                        frame.sequenceID,
                        frame.colorImage,
                        processedCVMat,
                        frame.type,
                        frame.frameStaticProperties),
                variance);
    }

    @Override
    public void release() {
        // we never actually need to give resources up since pipelinemanager only makes
        // one of us
    }
}
