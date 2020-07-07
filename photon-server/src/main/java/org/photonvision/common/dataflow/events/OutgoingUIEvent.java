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

package org.photonvision.common.dataflow.events;

import java.util.HashMap;
import org.photonvision.common.dataflow.DataChangeDestination;
import org.photonvision.common.dataflow.DataChangeSource;
import org.photonvision.server.UIUpdateType;

public class OutgoingUIEvent<T> extends DataChangeEvent<T> {
    public final UIUpdateType updateType;

    public OutgoingUIEvent(UIUpdateType updateType, String propertyName, T newValue) {
        super(DataChangeSource.DCS_WEBSOCKET, DataChangeDestination.DCD_UI, propertyName, newValue);
        this.updateType = updateType;
    }

    @SuppressWarnings("unchecked")
    public OutgoingUIEvent(UIUpdateType updateType, String dataKey, HashMap<String, Object> data) {
        this(updateType, dataKey, (T) data.get(dataKey));
    }
}
