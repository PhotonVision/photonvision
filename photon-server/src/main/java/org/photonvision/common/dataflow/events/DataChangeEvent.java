package org.photonvision.common.dataflow.events;

import org.photonvision.common.dataflow.DataChangeDestination;
import org.photonvision.common.dataflow.DataChangeSource;

public class DataChangeEvent<T> {
    public final DataChangeSource sourceType;
    public final DataChangeDestination destType;
    public final String propertyName;
    public final T data;

    public DataChangeEvent(
            DataChangeSource sourceType,
            DataChangeDestination destType,
            String propertyName,
            T newValue) {
        this.sourceType = sourceType;
        this.destType = destType;
        this.propertyName = propertyName;
        this.data = newValue;
    }

    @Override
    public String toString() {
        return "DataChangeEvent{"
                + "sourceType="
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
