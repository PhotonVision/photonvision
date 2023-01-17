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

import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.raspi.LibCameraJNI;
import org.photonvision.vision.camera.CameraQuirk;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.pipe.impl.CalculateFPSPipe;
import org.photonvision.vision.pipe.impl.Draw2dCrosshairPipe;
import org.photonvision.vision.pipe.impl.ResizeImagePipe;
import org.photonvision.vision.pipe.impl.RotateImagePipe;
import org.photonvision.vision.pipeline.result.DriverModePipelineResult;

public class DriverModePipeline
        extends CVPipeline<DriverModePipelineResult, DriverModePipelineSettings> {
    private final RotateImagePipe rotateImagePipe = new RotateImagePipe();
    private final Draw2dCrosshairPipe draw2dCrosshairPipe = new Draw2dCrosshairPipe();
    private final CalculateFPSPipe calculateFPSPipe = new CalculateFPSPipe();
    private final ResizeImagePipe resizeImagePipe = new ResizeImagePipe();

    private static final FrameThresholdType PROCESSING_TYPE = FrameThresholdType.NONE;

    public DriverModePipeline() {
        super(PROCESSING_TYPE);
        settings = new DriverModePipelineSettings();
    }

    public DriverModePipeline(DriverModePipelineSettings settings) {
        super(PROCESSING_TYPE);
        this.settings = settings;
    }

    @Override
    protected void setPipeParamsImpl() {
        RotateImagePipe.RotateImageParams rotateImageParams =
                new RotateImagePipe.RotateImageParams(settings.inputImageRotationMode);
        rotateImagePipe.setParams(rotateImageParams);

        Draw2dCrosshairPipe.Draw2dCrosshairParams draw2dCrosshairParams =
                new Draw2dCrosshairPipe.Draw2dCrosshairParams(
                        frameStaticProperties, settings.streamingFrameDivisor, settings.inputImageRotationMode);
        draw2dCrosshairPipe.setParams(draw2dCrosshairParams);

        resizeImagePipe.setParams(
                new ResizeImagePipe.ResizeImageParams(settings.streamingFrameDivisor));

        // if (LibCameraJNI.isSupported() && cameraQuirks.hasQuirk(CameraQuirk.PiCam)) {
        // LibCameraJNI.setRotation(settings.inputImageRotationMode.value);
        // LibCameraJNI.setShouldCopyColor(true);
        // }
    }

    @Override
    public DriverModePipelineResult process(Frame frame, DriverModePipelineSettings settings) {
        long totalNanos = 0;
        boolean accelerated = LibCameraJNI.isSupported() && cameraQuirks.hasQuirk(CameraQuirk.PiCam);

        // apply pipes
        var inputMat = frame.colorImage.getMat();

        boolean emptyIn = inputMat.empty();

        if (!emptyIn) {
            if (!accelerated) {
                var rotateImageResult = rotateImagePipe.run(inputMat);
                totalNanos += rotateImageResult.nanosElapsed;
            }

            totalNanos += resizeImagePipe.run(inputMat).nanosElapsed;

            var draw2dCrosshairResult = draw2dCrosshairPipe.run(Pair.of(inputMat, List.of()));

            // calculate elapsed nanoseconds
            totalNanos += draw2dCrosshairResult.nanosElapsed;
        }

        var fpsResult = calculateFPSPipe.run(null);
        var fps = fpsResult.output;

        // Flip-flop input and output in the Frame
        return new DriverModePipelineResult(
                MathUtils.nanosToMillis(totalNanos),
                fps,
                new Frame(frame.processedImage, frame.colorImage, frame.type, frame.frameStaticProperties));
    }
}
