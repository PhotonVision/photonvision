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

import java.util.ArrayList;
import java.util.List;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

import photonvision.core.proto.PhotonMessage.PhotonDataChangeEvent;

// Considered NT, but seems overkill. NewDataChangeService is <60 LOC
public class NewDataChangeService {
    // Incredibly cursed version of NtTopicSet. Hard-code so we're statically typed
    public static final NewDataChangeService VM_CHANGE_EVENTS = new NewDataChangeService();

    private static final Logger logger = new Logger(NewDataChangeService.class, LogGroup.WebServer);

    // Subscribers own their own queues of changes
    public static class NewDataChangeSubscriber {
        // Make sure access is syncronized
        // TODO length is unbounded
        private final List<PhotonDataChangeEvent> unprocessedEvents = new ArrayList<>();

        public void publish(PhotonDataChangeEvent event) {
            synchronized (unprocessedEvents) {
                unprocessedEvents.add(event);
            }
        }

        public final List<PhotonDataChangeEvent> getAndClearEvents() {
            synchronized (unprocessedEvents) {
                var copy = new ArrayList<>(unprocessedEvents);
                unprocessedEvents.clear();
                return copy;
            }
        }
    }

    // Syncronize access to this list
    private final List<NewDataChangeSubscriber> subscribers = new ArrayList<>();

    public NewDataChangeService() {
    }

    public NewDataChangeSubscriber subscribe() {
        var ret = new NewDataChangeSubscriber();
        synchronized (subscribers) {
            subscribers.add(ret);
        }
        return ret;
    }

    public void publish(PhotonDataChangeEvent event) {
        synchronized (subscribers) {
            for (var sub : subscribers) {
                sub.publish(event);
            }
        }
    }
}
