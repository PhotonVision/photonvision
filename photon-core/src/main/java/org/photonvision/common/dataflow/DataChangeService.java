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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import org.photonvision.common.dataflow.events.DataChangeEvent;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

@SuppressWarnings("rawtypes")
public class DataChangeService {
    private static final Logger logger = new Logger(DataChangeService.class, LogGroup.WebServer);

    private static class ThreadSafeSingleton {
        private static final DataChangeService INSTANCE = new DataChangeService();
    }

    public static DataChangeService getInstance() {
        return ThreadSafeSingleton.INSTANCE;
    }

    private final CopyOnWriteArrayList<DataChangeSubscriber> subscribers;

    @SuppressWarnings("FieldCanBeLocal")
    private final Thread dispatchThread;

    private final BlockingQueue<DataChangeEvent> eventQueue = new LinkedBlockingQueue<>();

    private DataChangeService() {
        subscribers = new CopyOnWriteArrayList<>();
        dispatchThread = new Thread(this::dispatchFromQueue);
        dispatchThread.setName("DataChangeEventDispatchThread");
        dispatchThread.start();
    }

    public boolean hasEvents() {
        return !eventQueue.isEmpty();
    }

    private void dispatchFromQueue() {
        while (true) {
            try {
                var taken = eventQueue.take();
                for (var sub : subscribers) {
                    if (sub.wantedSources.contains(taken.sourceType)
                            && sub.wantedDestinations.contains(taken.destType)) {
                        sub.onDataChangeEvent(taken);
                    }
                }
            } catch (Exception e) {
                logger.error("Exception when dispatching event!", e);
                e.printStackTrace();
            }
        }
    }

    public void addSubscriber(DataChangeSubscriber subscriber) {
        if (!subscribers.addIfAbsent(subscriber)) {
            logger.warn("Attempted to add already added subscriber!");
        } else {
            logger.debug(
                    () -> {
                        var sources =
                                subscriber.wantedSources.stream()
                                        .map(Enum::toString)
                                        .collect(Collectors.joining(", "));
                        var dests =
                                subscriber.wantedDestinations.stream()
                                        .map(Enum::toString)
                                        .collect(Collectors.joining(", "));

                        return "Added subscriber - " + "Sources: " + sources + ", Destinations: " + dests;
                    });
        }
    }

    public void addSubscribers(DataChangeSubscriber... subs) {
        for (var sub : subs) {
            addSubscriber(sub);
        }
    }

    public void publishEvent(DataChangeEvent event) {
        eventQueue.offer(event);
    }

    public void publishEvents(DataChangeEvent... events) {
        for (var event : events) {
            publishEvent(event);
        }
    }
}
