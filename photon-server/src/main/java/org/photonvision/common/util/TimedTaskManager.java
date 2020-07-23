package org.photonvision.common.util;

import org.jetbrains.annotations.NotNull;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class TimedTaskManager {

    private static final Logger logger = new Logger(TimedTaskManager.class, LogGroup.General);

    private static class Singleton {
        public static final TimedTaskManager INSTANCE = new TimedTaskManager();
    }

    public static TimedTaskManager getInstance() {
        return Singleton.INSTANCE;
    }

    private static class CaughtThreadFactory implements ThreadFactory {
       @Override
       public Thread newThread(@NotNull Runnable r) {
           return new Thread(() -> {
               try {
                   r.run();
               } catch (Throwable t) {
                   logger.error("Exception running task: ");
                   logger.error(Arrays.toString(t.getStackTrace()));
               }
           });
       }
    }

    private final ScheduledExecutorService taskExecutor = Executors.newScheduledThreadPool(2, new CaughtThreadFactory());

    public void addTask(Runnable runnable, long millisInterval) {
        taskExecutor.scheduleAtFixedRate(runnable, 0, millisInterval, TimeUnit.MILLISECONDS);
    }
}
