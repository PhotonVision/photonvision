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

package org.photonvision.common.dataflow.websocket;

import java.util.List;
import org.photonvision.common.configuration.NeuralNetworkPropertyManager;

public class UIGeneralSettings {
    public UIGeneralSettings(
            String version,
            String gpuAcceleration,
            boolean mrCalWorking,
            NeuralNetworkPropertyManager.ModelProperties[] availableModels,
            List<String> supportedBackends,
            String hardwareModel,
            String hardwarePlatform,
            boolean conflictingHostname,
            String conflictingCameras) {
        this.version = version;
        this.gpuAcceleration = gpuAcceleration;
        this.mrCalWorking = mrCalWorking;
        this.availableModels = availableModels;
        this.supportedBackends = supportedBackends;
        this.hardwareModel = hardwareModel;
        this.hardwarePlatform = hardwarePlatform;
        this.conflictingHostname = conflictingHostname;
        this.conflictingCameras = conflictingCameras;
    }

    public String version;
    public String gpuAcceleration;
    public boolean mrCalWorking;
    public NeuralNetworkPropertyManager.ModelProperties[] availableModels;
    public List<String> supportedBackends;
    public String hardwareModel;
    public String hardwarePlatform;
    public boolean conflictingHostname;
    public String conflictingCameras;
}
