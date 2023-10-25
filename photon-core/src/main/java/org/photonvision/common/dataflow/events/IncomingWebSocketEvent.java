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

public class IncomingWebSocketEvent<T> extends DataChangeEvent<T> {
    public final Integer cameraIndex;
    public final WsContext originContext;

    public IncomingWebSocketEvent(DataChangeDestination destType, String propertyName, T newValue) {
        this(destType, propertyName, newValue, null, null);
    }

    public IncomingWebSocketEvent(
            DataChangeDestination destType,
            String propertyName,
            T newValue,
            Integer cameraIndex,
            WsContext originContext) {
        super(DataChangeSource.DCS_WEBSOCKET, destType, propertyName, newValue);
        this.cameraIndex = cameraIndex;
        this.originContext = originContext;
    }

    @SuppressWarnings("unchecked")
    public IncomingWebSocketEvent(
            DataChangeDestination destType, String dataKey, HashMap<String, Object> data) {
        this(destType, dataKey, (T) data.get(dataKey));
    }

    @Override
    public String toString() {
        return "IncomingWebSocketEvent{"
                + "cameraIndex="
                + cameraIndex
                + ", sourceType="
                + sourceType
                + ", destType="
                + destType
                + ", propertyName='"
                + propertyName
                + '\''
                + ", data="
                + data
                + '}';
    }
}
