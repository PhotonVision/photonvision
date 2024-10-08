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
import org.photonvision.common.hardware.HardwareManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class UIInboundSubscriber extends DataChangeSubscriber {
    private static final Logger logger = new Logger(UIInboundSubscriber.class, LogGroup.General);

    public UIInboundSubscriber() {
        super(
                Collections.singletonList(DataChangeSource.DCS_WEBSOCKET),
                Collections.singletonList(DataChangeDestination.DCD_PROGRAM));
    }

    @Override
    public void onDataChangeEvent(DataChangeEvent<?> event) {
        if (event instanceof IncomingWebSocketEvent<?> incomingWSEvent) {
            switch (incomingWSEvent.propertyName) {
                case "userConnected" -> {
                    DataChangeService.getInstance()
                            .publishEvent(
                                    new OutgoingUIEvent<>(
                                            "fullsettings",
                                            ConfigManager.getInstance().getConfig().toHashMap(),
                                            incomingWSEvent.originContext));
                    Logger.sendConnectedBacklog();
                    NetworkTablesManager.getInstance().broadcastConnectedStatus();
                }
                case "ledPercentage" -> {
                    HardwareManager.getInstance().setBrightnessPercent((Integer) incomingWSEvent.data);
                }
                case "restartProgram" -> {
                    HardwareManager.getInstance().restartProgram();
                }
                case "restartDevice" -> {
                    HardwareManager.getInstance().restartDevice();
                }
                case "publishMetrics" -> {
                    HardwareManager.getInstance().publishMetrics();
                }
            }
        }
    }
}
