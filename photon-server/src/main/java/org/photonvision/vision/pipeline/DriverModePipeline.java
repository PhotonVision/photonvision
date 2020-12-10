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
import org.opencv.core.Mat;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.raspi.PicamJNI;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipe.impl.CalculateFPSPipe;
import org.photonvision.vision.pipe.impl.Draw2dCrosshairPipe;
import org.photonvision.vision.pipe.impl.RotateImagePipe;
import org.photonvision.vision.pipeline.result.DriverModePipelineResult;

public class DriverModePipeline
        extends CVPipeline<DriverModePipelineResult, DriverModePipelineSettings> {

    private final RotateImagePipe rotateImagePipe = new RotateImagePipe();
    private final Draw2dCrosshairPipe draw2dCrosshairPipe = new Draw2dCrosshairPipe();
    private final CalculateFPSPipe calculateFPSPipe = new CalculateFPSPipe();

    public DriverModePipeline() {
        settings = new DriverModePipelineSettings();
    }

    @Override
    protected void setPipeParams(
            FrameStaticProperties frameStaticProperties, DriverModePipelineSettings settings) {
        RotateImagePipe.RotateImageParams rotateImageParams =
                new RotateImagePipe.RotateImageParams(settings.inputImageRotationMode);
        rotateImagePipe.setParams(rotateImageParams);

        Draw2dCrosshairPipe.Draw2dCrosshairParams draw2dCrosshairParams =
                new Draw2dCrosshairPipe.Draw2dCrosshairParams(frameStaticProperties);
        draw2dCrosshairPipe.setParams(draw2dCrosshairParams);

        if (PicamJNI.isSupported()) {
            PicamJNI.setRotation(settings.inputImageRotationMode.value);
            PicamJNI.setShouldCopyColor(true);
        }
    }

    @Override
    public DriverModePipelineResult process(Frame frame, DriverModePipelineSettings settings) {
        // apply pipes
        var inputMat = frame.image.getMat();
        if (inputMat.channels() == 1 && PicamJNI.isSupported()) {
            long colorMatPtr = PicamJNI.grabFrame(true);
            if (colorMatPtr == 0) throw new RuntimeException("Got null Mat from GPU Picam driver");
            inputMat.release();
            inputMat = new Mat(colorMatPtr);
        }

        var rotateImageResult = rotateImagePipe.run(inputMat);
        var draw2dCrosshairResult = draw2dCrosshairPipe.run(Pair.of(inputMat, List.of()));

        // calculate elapsed nanoseconds
        long totalNanos = rotateImageResult.nanosElapsed + draw2dCrosshairResult.nanosElapsed;

        var fpsResult = calculateFPSPipe.run(null);
        var fps = fpsResult.output;

        return new DriverModePipelineResult(
                MathUtils.nanosToMillis(totalNanos),
                fps,
                new Frame(new CVMat(inputMat), frame.frameStaticProperties));
    }
}
