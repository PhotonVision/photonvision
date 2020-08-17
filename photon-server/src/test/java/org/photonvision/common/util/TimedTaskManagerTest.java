package org.photonvision.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class TimedTaskManagerTest {

    @Test
    public void atomicIntegerIncrementTest() throws InterruptedException {
        AtomicInteger i = new AtomicInteger();
        TimedTaskManager.getInstance().addTask("TaskManagerTest", i::getAndIncrement, 2);
        Thread.sleep(400);
        Assertions.assertEquals(200, i.get(), 5);
    }
}
