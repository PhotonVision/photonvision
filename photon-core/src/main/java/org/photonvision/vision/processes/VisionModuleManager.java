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

        return addedModules.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey)) // sort by stream index
                .map(Map.Entry::getValue) // map to Stream of VisionModule
                .collect(Collectors.toList()); // collect in a List
    }

    private void assignCameraIndex(List<VisionSource> config) {
        // We won't necessarily have already added all the cameras we need to at this point. By
        // operating on the list, we have a fairly good idea of which we need to change, but it's not
        // guaranteed that we change the correct one The best we can do is try to avoid a case where the
        // stream index runs away to infinity since we can only stream 5 cameras at once

        var currentSources = new ArrayList<VisionSource>();
        currentSources.addAll(
                this.getModules().stream().map(it -> it.visionSource).collect(Collectors.toList()));

        // Go through the current list of sources to make sure that none of them have
        // the same index as another already existing.
        for (int i = 0; i < currentSources.size(); i++) {
            var v = currentSources.get(i);
            var currentDevModified = new ArrayList<VisionSource>(currentSources);
            currentDevModified.remove(i);
            int idx = 0;
            if (currentDevModified.stream()
                    .anyMatch(
                            it -> it.getCameraConfiguration().streamIndex == v.cameraConfiguration.streamIndex)) {
                while (currentDevModified.stream()
                        .map(it -> it.getCameraConfiguration().streamIndex)
                        .collect(Collectors.toList())
                        .contains(idx)) {
                    idx++;
                }
                logger.warn(
                        v.cameraConfiguration.toString()
                                + " is using an already used stream index assigning new index idx: "
                                + idx);
                v.getCameraConfiguration().streamIndex = idx;
            }
        }

        var newSources = new ArrayList<VisionSource>(config);

        // Go through all of the new devices and see if their proposed index already
        // exists or if there are duplicate indexs in the new sources.
        for (int i = 0; i < newSources.size(); i++) {
            var v = newSources.get(i);
            var newDevModified = new ArrayList<VisionSource>(newSources);
            newDevModified.remove(i);
            int idx = 0;
            if (newDevModified.stream()
                            .anyMatch(
                                    it ->
                                            it.getCameraConfiguration().streamIndex == v.cameraConfiguration.streamIndex)
                    || currentSources.stream()
                            .anyMatch(
                                    t ->
                                            t.getCameraConfiguration().streamIndex
                                                    == v.cameraConfiguration.streamIndex)) {
                while (newDevModified.stream()
                                .map(it -> it.getCameraConfiguration().streamIndex)
                                .collect(Collectors.toList())
                                .contains(idx)
                        || currentSources.stream()
                                .map(it -> it.getCameraConfiguration().streamIndex)
                                .collect(Collectors.toList())
                                .contains(idx)) {
                    idx++;
                }
                logger.debug(
                        "Assigning new device "
                                + v.cameraConfiguration.toString()
                                + " a new index idx: "
                                + idx);
                v.getCameraConfiguration().streamIndex = idx;
            }
        }
    }
}
