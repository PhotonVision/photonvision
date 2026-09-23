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

import io.avaje.json.JsonDataException;
import org.photonvision.common.dataflow.NewDataChangeService;
import org.photonvision.common.dataflow.NewDataChangeService.NewDataChangeSubscriber;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import photonvision.core.proto.PhotonMessage.OutgoingDashboardEvent;

@SuppressWarnings("rawtypes")
/*
 * DO NOT use logging in this class. If you do, the logs will recurse forever!
 */
class UIOutboundSubscriber {
    Logger logger = new Logger(UIOutboundSubscriber.class, LogGroup.WebServer);

    private NewDataChangeSubscriber<OutgoingDashboardEvent> subscriber;

    private Thread listenerThread;

    public UIOutboundSubscriber() {
        this.subscriber = NewDataChangeService.OUTBOUND_UI_EVENTS.subscribe();

        this.listenerThread =
                new Thread(
                        () -> {
                            while (true) {
                                try {
                                    // Arbitrary limit. Processes queued events in batches
                                    Thread.sleep(100);
                                    update();
                                } catch (InterruptedException e) {
                                    logger.error("UIOutboundSubscriber thread interrupted!", e);
                                }
                            }
                        });
        this.listenerThread.start();
    }

    public void update() throws InterruptedException {
        var events = subscriber.waitForEvents();
        for (var event : events) {
            // TODO add originContext to event
            try {
                DataSocketHandler.getInstance().broadcastMessage(event, null);
            } catch (JsonDataException e) {
                logger.error("Failed to process outgoing message!", e);
            }
        }
    }
}
