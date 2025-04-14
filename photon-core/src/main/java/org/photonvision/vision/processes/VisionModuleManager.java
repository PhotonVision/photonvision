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
import org.photonvision.common.dataflow.websocket.UICameraConfiguration;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

/** VisionModuleManager has many VisionModules, and provides camera configuration data to them. */
public class VisionModuleManager {
    private final Logger logger = new Logger(VisionModuleManager.class, LogGroup.VisionModule);

    private final List<VisionModule> visionModules = new ArrayList<>();

    VisionModuleManager() {}

    public List<VisionModule> getModules() {
        return visionModules;
    }

    public VisionModule getModule(String uniqueName) {
        for (var module : visionModules) {
            if (module.getStateAsCameraConfig().uniqueName.equals(uniqueName)) return module;
        }
        return null;
    }

    public synchronized VisionModule addSource(VisionSource visionSource) {
        visionSource.cameraConfiguration.streamIndex = newCameraIndex();

        var pipelineManager = new PipelineManager(visionSource.getCameraConfiguration());
        var module = new VisionModule(pipelineManager, visionSource);
        visionModules.add(module);

        return module;
    }

    public synchronized void removeModule(VisionModule module) {
        visionModules.remove(module);
        module.stop();
        module.saveAndBroadcastAll();
    }

    private synchronized int newCameraIndex() {
        // We won't necessarily have already added all the cameras we need to at this point
        // But by operating on the list, we have a fairly good idea of which we need to change,
        // but it's not guaranteed that we change the correct one
        // The best we can do is try to avoid a case where the stream index runs away to infinity
        // since we can only stream 5 cameras at once

        // Big list, which should contain every vision source (currently loaded plus the new ones being
        // added)
        List<Integer> bigList =
                this.getModules().stream()
                        .map(it -> it.getCameraConfiguration().streamIndex)
                        .collect(Collectors.toList());

        int idx = 0;
        while (bigList.contains(idx)) {
            idx++;
        }

        if (idx >= 5) {
            logger.warn("VisionModuleManager has reached the maximum number of cameras (5).");
        }

        return idx;
    }

    public static class UiVmmState {
        public final List<UICameraConfiguration> visionModules;

        UiVmmState(List<UICameraConfiguration> _v) {
            this.visionModules = _v;
        }
    }

    public synchronized UiVmmState getState() {
        return new UiVmmState(
                this.visionModules.stream()
                        .map(VisionModule::toUICameraConfig)
                        .map(
                                it -> {
                                    it.calibrations = null;
                                    return it;
                                })
                        .collect(Collectors.toList()));
    }
}
