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
