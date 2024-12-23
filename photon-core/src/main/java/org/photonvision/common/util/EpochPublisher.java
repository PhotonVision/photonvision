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

package org.photonvision.common.util;

import edu.wpi.first.networktables.IntegerArrayPublisher;
import edu.wpi.first.networktables.StringArrayPublisher;
import org.photonvision.common.dataflow.networktables.NetworkTablesManager;

public class EpochPublisher {
    private IntegerArrayPublisher timePub;
    private StringArrayPublisher namePub;

    public EpochPublisher(String nickname) {
        this.timePub =
                NetworkTablesManager.getInstance()
                        .kRootTable
                        .getSubTable(nickname)
                        .getIntegerArrayTopic("tracer/timesUs")
                        .publish();
        this.namePub =
                NetworkTablesManager.getInstance()
                        .kRootTable
                        .getSubTable(nickname)
                        .getStringArrayTopic("tracer/epochNames")
                        .publish();
    }

    public void consume(OrderedTracer tracer) {
        timePub.set(tracer.getEpochTimes());
        namePub.set(tracer.getEpochNames());
    }
}
