package org.photonvision.common.dataflow;

import org.photonvision.common.dataflow.events.DataChangeEvent;

import java.util.List;
import java.util.Objects;

@SuppressWarnings("rawtypes")
public abstract class DataChangeSubscriber {
    public final List<DataChangeSource> wantedSources;
    public final List<DataChangeDestination> wantedDestinations;

    private final int hash;

    public DataChangeSubscriber(
            List<DataChangeSource> wantedSources, List<DataChangeDestination> wantedDestinations) {
        this.wantedSources = wantedSources;
        this.wantedDestinations = wantedDestinations;
        hash = Objects.hash(wantedSources, wantedDestinations);
    }

    public DataChangeSubscriber() {
        this(DataChangeSource.AllSources, DataChangeDestination.AllDestinations);
    }

    public DataChangeSubscriber(DataChangeSource.DataChangeSourceList wantedSources) {
        this(wantedSources, DataChangeDestination.AllDestinations);
    }

    public DataChangeSubscriber(DataChangeDestination.DataChangeDestinationList wantedDestinations) {
        this(DataChangeSource.AllSources, wantedDestinations);
    }

    public abstract void onDataChangeEvent(DataChangeEvent event);

    @Override
    public int hashCode() {
        return hash;
    }
}
