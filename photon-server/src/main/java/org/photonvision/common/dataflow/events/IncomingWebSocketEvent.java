package org.photonvision.common.dataflow.events;

import org.photonvision.common.dataflow.DataChangeDestination;
import org.photonvision.common.dataflow.DataChangeSource;
import java.util.HashMap;

public class IncomingWebSocketEvent<T> extends DataChangeEvent<T> {
    public IncomingWebSocketEvent(DataChangeDestination destType, String propertyName, T newValue) {
        super(DataChangeSource.DCS_WEBSOCKET, destType, propertyName, newValue);
    }

    @SuppressWarnings("unchecked")
    public IncomingWebSocketEvent(
            DataChangeDestination destType, String dataKey, HashMap<String, Object> data) {
        this(destType, dataKey, (T) data.get(dataKey));
    }
}
