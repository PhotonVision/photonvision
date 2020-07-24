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

    private static class CaughtThreadFactory implements ThreadFactory {
        private static final ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();

        @Override
        public Thread newThread(@NotNull Runnable r) {
            Thread thread = defaultThreadFactory.newThread(r);
            thread.setUncaughtExceptionHandler(
                    (t, e) -> {
                        logger.error("TimedTask threw uncaught exception!");
                        e.printStackTrace();
                    });
            return thread;
        }
    }

    private static class TimedTaskExecutorPool extends ScheduledThreadPoolExecutor {
        public TimedTaskExecutorPool(int corePoolSize) {
            super(corePoolSize, new CaughtThreadFactory());
        }

        // Thanks to Abdullah Ozturk for this tip
        // https://medium.com/@aozturk/how-to-handle-uncaught-exceptions-in-java-abf819347906
        @Override
        protected void afterExecute(Runnable runnable, Throwable throwable) {
            super.afterExecute(runnable, throwable);

            // If submit() method is called instead of execute()
            if (throwable == null && runnable instanceof Future<?>) {
                try {
                    //noinspection unused
                    Object result = ((Future<?>) runnable).get();
                } catch (CancellationException e) {
                    throwable = e;
                } catch (ExecutionException e) {
                    throwable = e.getCause();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            if (throwable != null) {
                logger.error("TimedTask threw uncaught exception!");
                throwable.printStackTrace();
                // Restart the runnable again
                execute(runnable);
            }
        }
    }

    private final ScheduledExecutorService timedTaskExecutorPool = new TimedTaskExecutorPool(2);
    private final ConcurrentHashMap<String, Future<?>> activeTasks = new ConcurrentHashMap<>();

    public void addTask(String identifier, Runnable runnable, long millisInterval) {
        if (!activeTasks.containsKey(identifier)) {
            var future =
                    timedTaskExecutorPool.scheduleAtFixedRate(
                            runnable, 0, millisInterval, TimeUnit.MILLISECONDS);
            activeTasks.put(identifier, future);
        }
    }

    public void addTask(
            String identifier, Runnable runnable, long millisStartDelay, long millisInterval) {
        if (!activeTasks.containsKey(identifier)) {
            var future =
                    timedTaskExecutorPool.scheduleAtFixedRate(
                            runnable, millisStartDelay, millisInterval, TimeUnit.MILLISECONDS);
            activeTasks.put(identifier, future);
        }
    }

    public void cancelTask(String identifier) {
        var future = activeTasks.getOrDefault(identifier, null);
        if (future != null) {
            future.cancel(true);
            activeTasks.remove(identifier);
        }
    }
}
