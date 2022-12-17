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

import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.util.Units;
import org.bytedeco.javacpp.Loader;
import org.opencv.aruco.DetectorParameters;
import org.opencv.core.Mat;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.raspi.PicamJNI;
import org.photonvision.vision.apriltag.AprilTagDetectorParams;
import org.photonvision.vision.apriltag.DetectionResult;
import org.photonvision.vision.aruco.ArucoDetectionResult;
import org.photonvision.vision.aruco.ArucoDetectorParams;
import org.photonvision.vision.camera.CameraQuirk;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipe.CVPipe.CVPipeResult;
import org.photonvision.vision.pipe.impl.*;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.TrackedTarget;
import org.photonvision.vision.target.TrackedTarget.TargetCalculationParameters;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("DuplicatedCode")
public class ArucoPipeline extends CVPipeline<CVPipelineResult, ArucoPipelineSettings> {
    private final RotateImagePipe rotateImagePipe = new RotateImagePipe();
    private final GrayscalePipe grayscalePipe = new GrayscalePipe();
    private final ArucoDetectionPipe arucoDetectionPipe = new ArucoDetectionPipe();
    private final CalculateFPSPipe calculateFPSPipe = new CalculateFPSPipe();
    DetectorParameters arucoDetectionParams = null;

    public ArucoPipeline() {
        settings = new ArucoPipelineSettings();
    }

    public ArucoPipeline(ArucoPipelineSettings settings) {
        this.settings = settings;
    }

    @Override
    protected void setPipeParamsImpl() {
        // Sanitize thread count - not supported to have fewer than 1 threads
        settings.threads = Math.max(1, settings.threads);

        RotateImagePipe.RotateImageParams rotateImageParams =
                new RotateImagePipe.RotateImageParams(settings.inputImageRotationMode);
        rotateImagePipe.setParams(rotateImageParams);

        if (cameraQuirks.hasQuirk(CameraQuirk.PiCam) && PicamJNI.isSupported()) {
            // TODO: Picam grayscale
            PicamJNI.setRotation(settings.inputImageRotationMode.value);
            PicamJNI.setShouldCopyColor(true); // need the color image to grayscale
        }


        arucoDetectionParams = ArucoDetectorParams.getDetectorParams(arucoDetectionParams, settings.decimate, settings.numIterations, settings.cornerAccuracy, settings.useAruco3);


        arucoDetectionPipe.setParams(
                new ArucoDetectionPipeParams(arucoDetectionParams,frameStaticProperties.cameraCalibration));


        arucoDetectionPipe.setParams(
                new ArucoDetectionPipeParams(arucoDetectionParams,frameStaticProperties.cameraCalibration));
    }

    @Override
    protected CVPipelineResult process(Frame frame, ArucoPipelineSettings settings) {
        long sumPipeNanosElapsed = 0L;
        Mat rawInputMat;
        boolean inputSingleChannel = frame.image.getMat().channels() == 1;

        if (inputSingleChannel) {
            rawInputMat = new Mat(PicamJNI.grabFrame(true));
            frame.image.getMat().release(); // release the 8bit frame ASAP.
        } else {
            rawInputMat = frame.image.getMat();
        }

        var inputFrame = new Frame(new CVMat(rawInputMat), frameStaticProperties);


        var outputFrame = new Frame(new CVMat(rawInputMat), frameStaticProperties);

        List<TrackedTarget> targetList;
        CVPipeResult<List<ArucoDetectionResult>> tagDetectionPipeResult;

        tagDetectionPipeResult = arucoDetectionPipe.run(rawInputMat);
        //sumPipeNanosElapsed += tagDetectionPipeResult.nanosElapsed;

        targetList = new ArrayList<>();
        for (ArucoDetectionResult detection : tagDetectionPipeResult.output) {
            // TODO this should be in a pipe, not in the top level here (Matt)

            // populate the target list
            // Challenge here is that TrackedTarget functions with OpenCV Contour
            TrackedTarget target =
                    new TrackedTarget(
                            detection,
                            new TargetCalculationParameters(
                                    false, null, null, null, null, frameStaticProperties));

            var correctedBestPose = target.getBestCameraToTarget3d();

            target.setBestCameraToTarget3d(
                    new Transform3d(correctedBestPose.getTranslation(), correctedBestPose.getRotation()));

            targetList.add(target);
        }

        var fpsResult = calculateFPSPipe.run(null);
        var fps = fpsResult.output;

        return new CVPipelineResult(sumPipeNanosElapsed, fps, targetList, outputFrame, inputFrame);
    }
}
