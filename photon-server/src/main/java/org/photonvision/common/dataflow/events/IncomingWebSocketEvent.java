package org.photonvision.common.dataflow.events;

import org.photonvision.common.dataflow.DataChangeDestination;
import org.photonvision.common.dataflow.DataChangeSource;
import java.util.HashMap;

public class IncomingWebSocketEvent<T> extends DataChangeEvent<T> {

    public final Integer cameraIndex;

    public IncomingWebSocketEvent(DataChangeDestination destType, String propertyName, T newValue) {
        this(destType, propertyName, newValue, null);
    }

    public IncomingWebSocketEvent(DataChangeDestination destType, String propertyName, T newValue, Integer cameraIndex) {
        super(DataChangeSource.DCS_WEBSOCKET, destType, propertyName, newValue);
        this.cameraIndex = cameraIndex;
    }

    @SuppressWarnings("unchecked")
    public IncomingWebSocketEvent(
            DataChangeDestination destType, String dataKey, HashMap<String, Object> data) {
        this(destType, dataKey, (T) data.get(dataKey));
    }

    @Override
    public String toString() {
        return "IncomingWebSocketEvent{" +
            "cameraIndex=" + cameraIndex +
            ", sourceType=" + sourceType +
            ", destType=" + destType +
            ", propertyName='" + propertyName + '\'' +
            ", data=" + data +
            '}';
    }
}
