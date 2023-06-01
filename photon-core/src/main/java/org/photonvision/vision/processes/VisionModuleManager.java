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

import java.util.*;
import java.util.stream.Collectors;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

/** VisionModuleManager has many VisionModules, and provides camera configuration data to them. */
public class VisionModuleManager {
    private final Logger logger = new Logger(VisionModuleManager.class, LogGroup.VisionModule);

    private static class ThreadSafeSingleton {
        private static final VisionModuleManager INSTANCE = new VisionModuleManager();
    }

    public static VisionModuleManager getInstance() {
        return VisionModuleManager.ThreadSafeSingleton.INSTANCE;
    }

    protected final List<VisionModule> visionModules = new ArrayList<>();

    VisionModuleManager() {}

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

    public List<VisionModule> addSources(List<VisionSource> visionSources) {
        var addedModules = new HashMap<Integer, VisionModule>();

        assignCameraIndex(visionSources);
        for (var visionSource : visionSources) {
            var pipelineManager = new PipelineManager(visionSource.getCameraConfiguration());

            var module = new VisionModule(pipelineManager, visionSource, visionModules.size());
            visionModules.add(module);
            addedModules.put(visionSource.getCameraConfiguration().streamIndex, module);
        }

        var sortedModulesList =
                addedModules.entrySet().stream()
                        .sorted(Comparator.comparingInt(Map.Entry::getKey)) // sort by stream index
                        .map(Map.Entry::getValue) // map to Stream of VisionModule
                        .collect(Collectors.toList()); // collect in a List

        return sortedModulesList;
    }

    private void assignCameraIndex(List<VisionSource> config) {
        // We won't necessarily have already added all of the cameras we need to at this point
        // But by operating on the list, we have a fairly good idea of which we need to change
        // but it's not guaranteed that we change the correct one
        // The best we can do is try to avoid a case where the stream index runs away to infinity
        // since we can only stream 5 cameras at once

        // Big list, which should contain every vision source (currently loaded plus the new ones being
        // added)
        var bigList = new ArrayList<VisionSource>();
        bigList.addAll(
                this.getModules().stream().map(it -> it.visionSource).collect(Collectors.toList()));
        bigList.addAll(config);

        for (var v : config) {
            var listNoV = new ArrayList<>(bigList);
            listNoV.remove(v);
            if (listNoV.stream()
                    .anyMatch(
                            it ->
                                    it.getCameraConfiguration().streamIndex
                                            == v.getCameraConfiguration().streamIndex)) {
                int idx = 0;
                while (listNoV.stream()
                        .map(it -> it.getCameraConfiguration().streamIndex)
                        .collect(Collectors.toList())
                        .contains(idx)) {
                    idx++;
                }
                logger.debug("Assigning idx " + idx);
                v.getCameraConfiguration().streamIndex = idx;
            }
        }
    }
}
