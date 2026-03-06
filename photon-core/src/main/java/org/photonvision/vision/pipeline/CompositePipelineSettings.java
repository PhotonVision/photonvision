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

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.photonvision.common.configuration.NeuralNetworkModelManager;
import org.photonvision.common.configuration.NeuralNetworkPropertyManager;
import org.photonvision.vision.apriltag.AprilTagFamily;
import org.photonvision.vision.objects.Model;
import org.photonvision.vision.target.TargetModel;

@JsonTypeName("CompositePipelineSettings")
public class CompositePipelineSettings extends AdvancedPipelineSettings {
    public boolean enableAprilTag = true;
    public boolean enableObjectDetection = true;

    // AprilTag settings
    public AprilTagFamily tagFamily = AprilTagFamily.kTag36h11;
    public int decimate = 1;
    public double blur = 0;
    public int threads = 4;
    public boolean debug = false;
    public boolean refineEdges = true;
    public int numIterations = 40;
    public int hammingDist = 0;
    public int decisionMargin = 35;
    public boolean doMultiTarget = false;
    public boolean doSingleTargetAlways = false;

    // Object detection settings
    public double confidence = 0.9;
    public double nms = 0.45;
    public NeuralNetworkPropertyManager.ModelProperties model;

    public CompositePipelineSettings() {
        super();
        pipelineType = PipelineType.Composite;
        outputShowMultipleTargets = true;
        targetModel = TargetModel.kAprilTag6p5in_36h11;
        cameraExposureRaw = 20;
        cameraAutoExposure = false;
        ledMode = false;
        model =
                NeuralNetworkModelManager.getInstance()
                        .getDefaultModel()
                        .map(Model::getProperties)
                        .orElse(null);
    }
}
