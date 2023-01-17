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

import java.util.Collections;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.dataflow.DataChangeDestination;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.DataChangeSource;
import org.photonvision.common.dataflow.DataChangeSubscriber;
import org.photonvision.common.dataflow.events.DataChangeEvent;
import org.photonvision.common.dataflow.events.IncomingWebSocketEvent;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;
import org.photonvision.common.dataflow.networktables.NetworkTablesManager;
import org.photonvision.common.logging.Logger;

public class UIInboundSubscriber extends DataChangeSubscriber {
    public UIInboundSubscriber() {
        super(
                Collections.singletonList(DataChangeSource.DCS_WEBSOCKET),
                Collections.singletonList(DataChangeDestination.DCD_GENSETTINGS));
    }

    @Override
    public void onDataChangeEvent(DataChangeEvent<?> event) {
        if (event instanceof IncomingWebSocketEvent) {
            var incomingWSEvent = (IncomingWebSocketEvent<?>) event;
            if (incomingWSEvent.propertyName.equals("userConnected")
                    || incomingWSEvent.propertyName.equals("sendFullSettings")) {
                // Send full settings
                var settings = ConfigManager.getInstance().getConfig().toHashMap();
                var message =
                        new OutgoingUIEvent<>("fullsettings", settings, incomingWSEvent.originContext);
                DataChangeService.getInstance().publishEvent(message);
                Logger.sendConnectedBacklog();
                NetworkTablesManager.getInstance().broadcastConnectedStatus();
            }
        }
    }
}
