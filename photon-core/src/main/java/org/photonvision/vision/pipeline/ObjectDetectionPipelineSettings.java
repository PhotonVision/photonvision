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
import org.photonvision.common.configuration.NeuralNetworkModelsSettings;
import org.photonvision.vision.objects.Model;
import org.photonvision.vision.target.TargetModel;

@JsonTypeName("ObjectDetectionPipelineSettings")
public class ObjectDetectionPipelineSettings extends AdvancedPipelineSettings {
    public double confidence;
    public double nms; // non maximal suppression
    public NeuralNetworkModelsSettings.ModelProperties model;

    public ObjectDetectionPipelineSettings() {
        super();
        this.pipelineType = PipelineType.ObjectDetection; // TODO: FIX this
        this.outputMaximumTargets = 20;
        cameraExposureRaw = 20;
        cameraAutoExposure = false;
        ledMode = false;
        // Use a spherical ball model by default: YOLO primarily detects game pieces like
        // balls/cargo, which are symmetric from all sides and work well with solvePNP.
        targetModel = TargetModel.k2025Algae;
        confidence = .9;
        nms = .45;
        model =
                NeuralNetworkModelManager.getInstance()
                        .getDefaultModel()
                        .map(Model::getProperties)
                        .orElse(null);
    }
}