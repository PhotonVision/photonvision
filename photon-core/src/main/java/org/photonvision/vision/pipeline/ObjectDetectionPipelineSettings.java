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
import org.photonvision.common.configuration.NeuralNetworkModelManager;

public class ObjectDetectionPipelineSettings extends AdvancedPipelineSettings {
    public double confidence;
    public double nms; // non maximal suppression
    public String model;

    public List<String> availableModels;

    public ObjectDetectionPipelineSettings() {
        super();
        this.pipelineType = PipelineType.ObjectDetection; // TODO: FIX this
        this.outputShowMultipleTargets = true;
        this.cameraExposure = 20;
        this.cameraAutoExposure = false;
        this.ledMode = false;
        confidence = .9;
        nms = .45;
        model = NeuralNetworkModelManager.getInstance().getDefaultRknnModel().getName();
        availableModels = NeuralNetworkModelManager.getInstance().getModels();
    }
}
