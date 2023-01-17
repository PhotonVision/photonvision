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

package org.photonvision.common.dataflow;

import java.util.List;
import java.util.Objects;
import org.photonvision.common.dataflow.events.DataChangeEvent;

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

    public abstract void onDataChangeEvent(DataChangeEvent<?> event);

    @Override
    public int hashCode() {
        return hash;
    }
}
