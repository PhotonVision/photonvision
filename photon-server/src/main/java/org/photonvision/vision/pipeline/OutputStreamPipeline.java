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

import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.DualOffsetValues;
import org.photonvision.vision.pipe.impl.Draw2dCrosshairPipe;
import org.photonvision.vision.pipe.impl.Draw2dTargetsPipe;
import org.photonvision.vision.pipe.impl.Draw3dTargetsPipe;
import org.photonvision.vision.pipe.impl.OutputMatPipe;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.TrackedTarget;

/**
* This is a "fake" pipeline that is just used to move identical pipe sets out of real pipelines. It
* shall not get its settings saved, nor shall it be managed by PipelineManager
*/
public class OutputStreamPipeline {

    private final OutputMatPipe outputMatPipe = new OutputMatPipe();
    private final Draw2dCrosshairPipe draw2dCrosshairPipe = new Draw2dCrosshairPipe();
    private final Draw2dTargetsPipe draw2dTargetsPipe = new Draw2dTargetsPipe();
    private final Draw3dTargetsPipe draw3dTargetsPipe = new Draw3dTargetsPipe();

    private final long[] pipeProfileNanos = new long[10];

    protected void setPipeParams(
            FrameStaticProperties frameStaticProperties, AdvancedPipelineSettings settings) {

        var dualOffsetValues =
                new DualOffsetValues(
                        settings.offsetDualPointA,
                        settings.offsetDualPointAArea,
                        settings.offsetDualPointB,
                        settings.offsetDualPointBArea);

        var draw2DTargetsParams =
                new Draw2dTargetsPipe.Draw2dTargetsParams(
                        settings.outputShouldDraw, settings.outputShowMultipleTargets);
        draw2dTargetsPipe.setParams(draw2DTargetsParams);

        var draw2dCrosshairParams =
                new Draw2dCrosshairPipe.Draw2dCrosshairParams(
                        settings.outputShouldDraw,
                        settings.offsetRobotOffsetMode,
                        settings.offsetSinglePoint,
                        dualOffsetValues,
                        frameStaticProperties);
        draw2dCrosshairPipe.setParams(draw2dCrosshairParams);

        var draw3dTargetsParams =
                new Draw3dTargetsPipe.Draw3dContoursParams(
                        settings.outputShouldDraw,
                        frameStaticProperties.cameraCalibration,
                        settings.targetModel);
        draw3dTargetsPipe.setParams(draw3dTargetsParams);
    }

    public CVPipelineResult process(
            Frame inputFrame,
            Frame outputFrame,
            AdvancedPipelineSettings settings,
            List<TrackedTarget> targetsToDraw,
            double fpsToDraw) {
        setPipeParams(inputFrame.frameStaticProperties, settings);
        var inMat = inputFrame.image.getMat();
        var outMat = outputFrame.image.getMat();

        long sumPipeNanosElapsed = 0L;

        // Convert single-channel HSV output mat to 3-channel BGR in preparation for streaming
        var outputMatPipeResult = outputMatPipe.run(outMat);
        sumPipeNanosElapsed += pipeProfileNanos[0] = outputMatPipeResult.nanosElapsed;

        // Draw 2D Crosshair on input and output
        var draw2dCrosshairResultOnInput = draw2dCrosshairPipe.run(Pair.of(inMat, targetsToDraw));
        sumPipeNanosElapsed += pipeProfileNanos[1] = draw2dCrosshairResultOnInput.nanosElapsed;

        var draw2dCrosshairResultOnOutput = draw2dCrosshairPipe.run(Pair.of(inMat, targetsToDraw));
        sumPipeNanosElapsed += pipeProfileNanos[2] = draw2dCrosshairResultOnOutput.nanosElapsed;

        // Draw 2D contours on input and output
        var draw2dTargetsOnInput =
                draw2dTargetsPipe.run(Triple.of(inMat, targetsToDraw, (int) fpsToDraw));
        sumPipeNanosElapsed += pipeProfileNanos[3] = draw2dTargetsOnInput.nanosElapsed;

        var draw2dTargetsOnOutput =
                draw2dTargetsPipe.run(Triple.of(outMat, targetsToDraw, (int) fpsToDraw));
        sumPipeNanosElapsed += pipeProfileNanos[4] = draw2dTargetsOnOutput.nanosElapsed;

        // Draw 3D Targets on input and output if necessary
        if (settings.solvePNPEnabled) {
            var drawOnInputResult = draw3dTargetsPipe.run(Pair.of(inMat, targetsToDraw));
            sumPipeNanosElapsed += pipeProfileNanos[5] = drawOnInputResult.nanosElapsed;

            var drawOnOutputResult = draw3dTargetsPipe.run(Pair.of(outMat, targetsToDraw));
            sumPipeNanosElapsed += pipeProfileNanos[6] = drawOnOutputResult.nanosElapsed;
        } else {
            pipeProfileNanos[5] = 0;
            pipeProfileNanos[6] = 0;
        }

        return new CVPipelineResult(
                MathUtils.nanosToMillis(sumPipeNanosElapsed),
                targetsToDraw,
                new Frame(new CVMat(outMat), outputFrame.frameStaticProperties),
                new Frame(new CVMat(inMat), inputFrame.frameStaticProperties));
    }
}
