package org.photonvision.server;

import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.dataflow.DataChangeDestination;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.DataChangeSource;
import org.photonvision.common.dataflow.DataChangeSubscriber;
import org.photonvision.common.dataflow.events.DataChangeEvent;
import org.photonvision.common.dataflow.events.IncomingWebSocketEvent;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;

import java.util.Collections;

public class UIInboundSubscriber extends DataChangeSubscriber {

    public UIInboundSubscriber() {
        super(Collections.singletonList(DataChangeSource.DCS_WEBSOCKET), Collections.singletonList(DataChangeDestination.DCD_GENSETTINGS));
    }

    @Override
    public void onDataChangeEvent(DataChangeEvent event) {
        if (event instanceof IncomingWebSocketEvent) {
            var incomingWSEvent = (IncomingWebSocketEvent) event;
            if (incomingWSEvent.propertyName.equals("userConnected") ||
                incomingWSEvent.propertyName.equals("sendFullSettings")) {
                // Send full settings
                var settings = ConfigManager.getInstance().getConfig().toHashMap();
                var message = new OutgoingUIEvent<>(UIUpdateType.BROADCAST, "fullsettings", settings);
                DataChangeService.getInstance().publishEvent(message);
            }
        }
    }
}
