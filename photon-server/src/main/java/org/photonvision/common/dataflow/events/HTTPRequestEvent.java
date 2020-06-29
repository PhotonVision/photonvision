package org.photonvision.common.dataflow.events;

import org.photonvision.common.dataflow.DataChangeDestination;
import org.photonvision.common.dataflow.DataChangeSource;

public class HTTPRequestEvent<T> extends DataChangeEvent<T> {
    public HTTPRequestEvent(
            DataChangeSource sourceType,
            DataChangeDestination destType,
            String propertyName,
            T newValue) {
        super(sourceType, destType, propertyName, newValue);
    }
}
