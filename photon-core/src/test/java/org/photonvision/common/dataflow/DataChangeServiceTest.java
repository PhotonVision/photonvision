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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.photonvision.common.dataflow.events.DataChangeEvent;

public class DataChangeServiceTest {
    private static DataChangeSubscriber noopSubscriber() {
        return new DataChangeSubscriber() {
            @Override
            public <T> void onDataChangeEvent(DataChangeEvent<T> event) {}
        };
    }

    /**
     * Stopping a handle must remove exactly its own subscriber. The old handle stored a list index
     * and nulled that slot; once any earlier subscriber was removed the stored index went stale and
     * could detach the wrong subscriber. Removal is now by object identity.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void stopRemovesSubscriberByIdentityNotIndex() throws Exception {
        var service = DataChangeService.getInstance();

        Field field = DataChangeService.class.getDeclaredField("subscribers");
        field.setAccessible(true);
        var subscribers = (List<DataChangeSubscriber>) field.get(service);

        var first = noopSubscriber();
        var second = noopSubscriber();
        var firstHandle = service.addSubscriber(first);
        var secondHandle = service.addSubscriber(second);
        try {
            assertTrue(subscribers.contains(first));
            assertTrue(subscribers.contains(second));

            firstHandle.stop();

            assertFalse(subscribers.contains(first), "the stopped subscriber should be removed");
            assertTrue(subscribers.contains(second), "other subscribers should stay registered");
        } finally {
            // Don't leak our subscribers into the process-wide singleton for later tests.
            firstHandle.stop();
            secondHandle.stop();
        }
    }
}
