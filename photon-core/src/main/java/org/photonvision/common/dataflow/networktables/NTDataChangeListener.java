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

package org.photonvision.common.dataflow.networktables;

import edu.wpi.first.networktables.NetworkTableEvent;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.Subscriber;
import java.util.EnumSet;
import java.util.function.Consumer;

public class NTDataChangeListener {
    private final NetworkTableInstance instance;
    private final Subscriber watchedEntry;
    private final int listenerID;

    public NTDataChangeListener(
            NetworkTableInstance instance,
            Subscriber watchedSubscriber,
            Consumer<NetworkTableEvent> dataChangeConsumer) {
        this.watchedEntry = watchedSubscriber;
        this.instance = instance;
        listenerID =
                this.instance.addListener(
                        watchedEntry, EnumSet.of(NetworkTableEvent.Kind.kValueAll), dataChangeConsumer);
    }

    public void remove() {
        this.instance.removeListener(listenerID);
    }
}
