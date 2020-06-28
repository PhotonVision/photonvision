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

package org.photonvision.vision.processes;

import org.photonvision.vision.pipeline.CVPipelineSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** VisionModuleManager has many VisionModules, and provides camera configuration data to them. */
public class VisionModuleManager {
    protected final List<VisionModule> visionModules = new ArrayList<>();

    public VisionModuleManager(HashMap<VisionSource, List<CVPipelineSettings>> visionSources) {
        for (var entry : visionSources.entrySet()) {
            var visionSource = entry.getKey();
            var pipelineManager = new PipelineManager(entry.getValue());
            var module = new VisionModule(pipelineManager, visionSource);
            visionModules.add(module);
            // todo: logging
        }
    }

    public void startModules() {
        for (var visionModule : visionModules) {
            visionModule.start();
            // todo: logging
        }
    }
}
