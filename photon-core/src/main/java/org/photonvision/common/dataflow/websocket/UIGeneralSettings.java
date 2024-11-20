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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UIGeneralSettings {
    public UIGeneralSettings(
            String version,
            String gpuAcceleration,
            boolean mrCalWorking,
            Map<String, ArrayList<String>> availableModels,
            List<String> supportedBackends,
            String hardwareModel,
            String hardwarePlatform) {
        this.version = version;
        this.gpuAcceleration = gpuAcceleration;
        this.mrCalWorking = mrCalWorking;
        this.availableModels = availableModels;
        this.supportedBackends = supportedBackends;
        this.hardwareModel = hardwareModel;
        this.hardwarePlatform = hardwarePlatform;
    }

    public String version;
    public String gpuAcceleration;
    public boolean mrCalWorking;
    public Map<String, ArrayList<String>> availableModels;
    public List<String> supportedBackends;
    public String hardwareModel;
    public String hardwarePlatform;
}
