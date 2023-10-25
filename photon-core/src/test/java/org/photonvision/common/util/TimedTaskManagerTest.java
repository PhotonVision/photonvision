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

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TimedTaskManagerTest {
    @Test
    public void atomicIntegerIncrementTest() throws InterruptedException {
        AtomicInteger i = new AtomicInteger();
        TimedTaskManager.getInstance().addTask("TaskManagerTest", i::getAndIncrement, 2);
        Thread.sleep(400);
        Assertions.assertEquals(200, i.get(), 15);
    }
}
