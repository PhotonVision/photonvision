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

import org.photonvision.common.dataflow.DataChangeSubscriber;
import org.photonvision.common.dataflow.events.DataChangeEvent;
import org.photonvision.common.dataflow.events.IncomingWebSocketEvent;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

@SuppressWarnings("unchecked")
public class VisionModuleChangeSubscriber extends DataChangeSubscriber {
    private final VisionModule parentModule;
    private final Logger logger;

    public VisionModuleChangeSubscriber(VisionModule parentModule) {
        this.parentModule = parentModule;
        logger =
                new Logger(
                        VisionModuleChangeSubscriber.class,
                        parentModule.visionSource.getSettables().getConfiguration().nickname,
                        LogGroup.VisionModule);
    }

    @Override
    public void onDataChangeEvent(DataChangeEvent<?> event) {
        if (event instanceof IncomingWebSocketEvent) {
            var wsEvent = (IncomingWebSocketEvent<?>) event;

            // Camera index -1 means a "multicast event" (i.e. the event is received by all cameras)
            if (wsEvent.cameraIndex != null
                    && (wsEvent.cameraIndex == parentModule.moduleIndex || wsEvent.cameraIndex == -1)) {
                logger.trace("Got PSC event - propName: " + wsEvent.propertyName);
                synchronized (parentModule.getSettingChanges()) {
                    parentModule
                            .getSettingChanges()
                            .add(
                                    new VisionModuleChange(
                                            wsEvent.propertyName,
                                            wsEvent.data,
                                            parentModule.pipelineManager.getCurrentPipeline().getSettings(),
                                            wsEvent.originContext));
                }
            }
        }
    }
}
