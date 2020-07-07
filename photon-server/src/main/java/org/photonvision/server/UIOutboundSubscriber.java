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

package org.photonvision.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Collections;
import java.util.HashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.photonvision.common.dataflow.DataChangeDestination;
import org.photonvision.common.dataflow.DataChangeSource;
import org.photonvision.common.dataflow.DataChangeSubscriber;
import org.photonvision.common.dataflow.events.DataChangeEvent;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

@SuppressWarnings("rawtypes")
/*
* DO NOT use logging in this class. If you do, the logs will recuse forever!
*/
class UIOutboundSubscriber extends DataChangeSubscriber {
    Logger logger = new Logger(UIOutboundSubscriber.class, LogGroup.Server);

    private final SocketHandler socketHandler;

    public UIOutboundSubscriber(SocketHandler socketHandler) {
        super(DataChangeSource.AllSources, Collections.singletonList(DataChangeDestination.DCD_UI));
        this.socketHandler = socketHandler;
    }

    @Override
    public void onDataChangeEvent(DataChangeEvent event) {
        if (event instanceof OutgoingUIEvent) {
            var thisEvent = (OutgoingUIEvent) event;
            try {
                switch (thisEvent.updateType) {
                    case BROADCAST:
                        {
                            //                        logger.debug("Broadcasting message");
                            if (event.data instanceof HashMap) {
                                var data = (HashMap) event.data;
                                socketHandler.broadcastMessage(data, null);
                            } else {
                                socketHandler.broadcastMessage(event.data, null);
                            }
                            break;
                        }
                    case SINGLEUSER:
                        {
                            //                        logger.debug("Sending single user message");
                            if (event.data instanceof Pair) {
                                var pair = (SocketHandler.SelectiveBroadcastPair) event.data;
                                socketHandler.broadcastMessage(pair.getLeft(), pair.getRight());
                            }
                            break;
                        }
                }
            } catch (JsonProcessingException e) {
                // TODO: Log
                e.printStackTrace();
            }
        }
    }
}
