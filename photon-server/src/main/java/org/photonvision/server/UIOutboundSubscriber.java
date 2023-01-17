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

package org.photonvision.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Collections;
import java.util.HashMap;
import org.photonvision.common.dataflow.DataChangeDestination;
import org.photonvision.common.dataflow.DataChangeSource;
import org.photonvision.common.dataflow.DataChangeSubscriber;
import org.photonvision.common.dataflow.events.DataChangeEvent;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

@SuppressWarnings("rawtypes")
/*
 * DO NOT use logging in this class. If you do, the logs will recurse forever!
 */
class UIOutboundSubscriber extends DataChangeSubscriber {
    Logger logger = new Logger(UIOutboundSubscriber.class, LogGroup.WebServer);

    private final DataSocketHandler socketHandler;

    public UIOutboundSubscriber(DataSocketHandler socketHandler) {
        super(DataChangeSource.AllSources, Collections.singletonList(DataChangeDestination.DCD_UI));
        this.socketHandler = socketHandler;
    }

    @Override
    public void onDataChangeEvent(DataChangeEvent event) {
        if (event instanceof OutgoingUIEvent) {
            var thisEvent = (OutgoingUIEvent) event;
            try {
                if (event.data instanceof HashMap) {
                    var data = (HashMap) event.data;
                    socketHandler.broadcastMessage(data, thisEvent.originContext);
                } else {
                    socketHandler.broadcastMessage(event.data, thisEvent.originContext);
                }
            } catch (JsonProcessingException e) {
                logger.error("Failed to process outgoing message!", e);
            }
        }
    }
}
