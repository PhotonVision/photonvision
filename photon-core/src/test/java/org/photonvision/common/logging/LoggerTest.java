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

package org.photonvision.common.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import org.junit.jupiter.api.Test;

public class LoggerTest {
    /**
     * The UI backlog buffers log messages until a websocket client connects. Before the fix it was
     * an unbounded list, so on a headless robot (no UI ever connecting) it grew without limit; it
     * must now cap at MAX_UI_BACKLOG_SIZE (500), dropping the oldest entries.
     */
    @Test
    public void uiBacklogIsBoundedTo500() throws Exception {
        // Reset the shared static state so the assertion holds regardless of test order.
        Field connected = Logger.class.getDeclaredField("connected");
        connected.setAccessible(true);
        connected.setBoolean(null, false);

        Field backlogField = Logger.class.getDeclaredField("uiBacklog");
        backlogField.setAccessible(true);
        var backlog = (ArrayDeque<?>) backlogField.get(null);
        synchronized (backlog) {
            backlog.clear();
        }

        var logger = new Logger(LoggerTest.class, LogGroup.General);
        for (int i = 0; i < 600; i++) {
            logger.error("ui backlog bound test " + i);
        }

        assertEquals(500, backlog.size(), "UI backlog must be bounded to 500 entries");
    }
}
