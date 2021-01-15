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

package org.photonvision.vision.processes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.photonvision.common.configuration.CameraConfiguration;

/** VisionModuleManager has many VisionModules, and provides camera configuration data to them. */
public class VisionModuleManager {

    private static class ThreadSafeSingleton {
        private static final VisionModuleManager INSTANCE = new VisionModuleManager();
    }

    public static VisionModuleManager getInstance() {
        return VisionModuleManager.ThreadSafeSingleton.INSTANCE;
    }

    protected final List<VisionModule> visionModules = new ArrayList<>();

    private VisionModuleManager() {}

    public List<VisionModule> getModules() {
        return visionModules;
    }

    public VisionModule getModule(String nickname) {
        for (var module : visionModules) {
            if (module.getStateAsCameraConfig().nickname.equals(nickname)) return module;
        }
        return null;
    }

    public VisionModule getModule(int i) {
        return visionModules.get(i);
    }

    public List<VisionModule> addSources(HashMap<VisionSource, CameraConfiguration> visionSources) {
        var addedModules = new ArrayList<VisionModule>();
        for (var entry : visionSources.entrySet()) {
            var visionSource = entry.getKey();
            var pipelineManager = new PipelineManager(entry.getValue());

            assignCameraIndex(visionSource.getSettables().getConfiguration());

            var module = new VisionModule(pipelineManager, visionSource, visionModules.size());
            visionModules.add(module);
            addedModules.add(module);
        }
        return addedModules;
    }

    private void assignCameraIndex(CameraConfiguration config) {
        var max =
                visionModules.stream()
                        .mapToInt(it -> it.visionSource.getSettables().getConfiguration().streamIndex)
                        .max()
                        .orElse(-1);

        // If the current stream index is reserved, increase by 1
        if (config.streamIndex <= max) {
            config.streamIndex = max + 1;
        }
    }
}
