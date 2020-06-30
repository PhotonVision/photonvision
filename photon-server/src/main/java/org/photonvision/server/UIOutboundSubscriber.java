package org.photonvision.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.tuple.Pair;
import org.photonvision.common.dataflow.DataChangeDestination;
import org.photonvision.common.dataflow.DataChangeSource;
import org.photonvision.common.dataflow.DataChangeSubscriber;
import org.photonvision.common.dataflow.events.DataChangeEvent;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

import java.util.Collections;
import java.util.HashMap;

@SuppressWarnings("rawtypes")
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
                    case BROADCAST: {
                        logger.debug("Broadcasting message");
                        if (event.data instanceof HashMap) {
                            var data = (HashMap) event.data;
                            socketHandler.broadcastMessage(data, null);
                        } else {
                            socketHandler.broadcastMessage(event.data, null);
                        }
                        break;
                    }
                    case SINGLEUSER: {
                        logger.debug("Sending single user message");
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
