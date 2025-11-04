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

import edu.wpi.first.math.Pair;
import java.util.List;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.pipe.impl.CalculateFPSPipe;
import org.photonvision.vision.pipe.impl.Draw2dCrosshairPipe;
import org.photonvision.vision.pipe.impl.ResizeImagePipe;
import org.photonvision.vision.pipe.impl.FocusPipe;
import org.photonvision.vision.opencv.CVMat;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.photonvision.vision.pipeline.result.FocusPipelineResult;
import org.photonvision.vision.pipeline.result.CVPipelineResult;


public class FocusPipeline extends CVPipeline<FocusPipelineResult, FocusPipelineSettings> {
    private final Draw2dCrosshairPipe draw2dCrosshairPipe = new Draw2dCrosshairPipe();
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
        draw2dCrosshairPipe.setParams(
                new Draw2dCrosshairPipe.Draw2dCrosshairParams(
                        frameStaticProperties,
                        settings.streamingFrameDivisor,
                        settings.inputImageRotationMode));

        resizeImagePipe.setParams(
                new ResizeImagePipe.ResizeImageParams(settings.streamingFrameDivisor));
    }

    @Override
    public FocusPipelineResult process(Frame frame, FocusPipelineSettings settings) {
       long totalNanos = 0;

        // apply pipes
        var inputMat = frame.colorImage.getMat();

        boolean emptyIn = inputMat.empty();

        if (!emptyIn) {
            totalNanos += resizeImagePipe.run(inputMat).nanosElapsed;

            if (true) {
                var draw2dCrosshairResult = draw2dCrosshairPipe.run(Pair.of(inputMat, List.of()));

                // calculate elapsed nanoseconds
                totalNanos += draw2dCrosshairResult.nanosElapsed;
            }
        }

        Mat displayMat = new Mat();
        // Run the focus pipe to compute Laplacian/variance overlay and get its result
        if (!emptyIn) {
            var focusResult = focusPipe.run(inputMat);
            totalNanos += focusResult.nanosElapsed;

            // focusResult.output may be CV_64F single-channel; convert to 8-bit for display
            Core.convertScaleAbs(focusResult.output, displayMat);
        }

        var fpsResult = calculateFPSPipe.run(null);
        var fps = fpsResult.output;

    // Flip-flop input and output in the Frame. Ensure processedImage contains the
    // focus-display mat (8-bit) so the output stream has image data.
    var processedCVMat = displayMat.empty() ? frame.processedImage : new CVMat(displayMat);

    return new FocusPipelineResult(
        frame.sequenceID,
        MathUtils.nanosToMillis(totalNanos),
        fps,
        new Frame(
            frame.sequenceID,
            frame.processedImage,
            processedCVMat,
            frame.type,
            frame.frameStaticProperties));
    }

    @Override
    public void release() {
        // we never actually need to give resources up since pipelinemanager only makes
        // one of us
    }
}
