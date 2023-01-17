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

package org.photonvision.common.dataflow.events;

import io.javalin.websocket.WsContext;
import java.util.HashMap;
import org.photonvision.common.dataflow.DataChangeDestination;
import org.photonvision.common.dataflow.DataChangeSource;

public class OutgoingUIEvent<T> extends DataChangeEvent<T> {
    public final WsContext originContext;

    public OutgoingUIEvent(String propertyName, T newValue) {
        this(propertyName, newValue, null);
    }

    public OutgoingUIEvent(String propertyName, T newValue, WsContext originContext) {
        super(DataChangeSource.DCS_WEBSOCKET, DataChangeDestination.DCD_UI, propertyName, newValue);
        this.originContext = originContext;
    }

    public static OutgoingUIEvent<HashMap<String, Object>> wrappedOf(
            String commandName, Object value) {
        HashMap<String, Object> data = new HashMap<>();
        data.put(commandName, value);
        return new OutgoingUIEvent<>(commandName, data);
    }

    public static OutgoingUIEvent<HashMap<String, Object>> wrappedOf(
            String commandName, String propertyName, Object value, WsContext originContext) {
        HashMap<String, Object> data = new HashMap<>();
        data.put(propertyName, value);

        return new OutgoingUIEvent<>(commandName, data, originContext);
    }
}
