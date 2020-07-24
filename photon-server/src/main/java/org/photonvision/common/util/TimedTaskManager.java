/*
 * Copyright (C) 2020 Photon Vision.
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

import java.util.Arrays;
import java.util.concurrent.*;
import org.jetbrains.annotations.NotNull;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class TimedTaskManager {

    private static final Logger logger = new Logger(TimedTaskManager.class, LogGroup.General);

    private static class Singleton {
        public static final TimedTaskManager INSTANCE = new TimedTaskManager();
    }

    public static TimedTaskManager getInstance() {
        return Singleton.INSTANCE;
    }

    private static class TimedTask {
        final String identifier;
        final Runnable runnable;
        final Future future;

        TimedTask(String identifier, Runnable runnable, Future future) {
            this.identifier = identifier;
            this.runnable = runnable;
            this.future = future;
        }
    }

    private class CaughtThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(@NotNull Runnable r) {
            String taskIdentifier = "Unknown";
            for (TimedTask timedTask : activeTasks.values()) {
                if (timedTask.runnable == r) {
                    taskIdentifier = timedTask.identifier;
                    break;
                }
            }

            var errorString = "Exception running task \"" + taskIdentifier + "\": ";
            return new Thread(
                    () -> {
                        try {
                            r.run();
                        } catch (Throwable t) {
                            logger.error(errorString);
                            logger.error(Arrays.toString(t.getStackTrace()));
                        }
                    });
        }
    }

    private final ScheduledExecutorService taskExecutor =
            Executors.newScheduledThreadPool(2, new CaughtThreadFactory());
    private final ConcurrentHashMap<String, TimedTask> activeTasks = new ConcurrentHashMap<>();

    public void addTask(String identifier, Runnable runnable, long millisInterval) {
        if (!activeTasks.containsKey(identifier)) {
            var future =
                    taskExecutor.scheduleAtFixedRate(runnable, 0, millisInterval, TimeUnit.MILLISECONDS);
            activeTasks.put(identifier, new TimedTask(identifier, runnable, future));
        }
    }

    public void cancelTask(String identifier) {
        var task = activeTasks.getOrDefault(identifier, null);
        if (task != null) {
            task.future.cancel(true);
            activeTasks.remove(task.identifier);
        }
    }
}
