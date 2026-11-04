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

package org.photonvision.common;

import static org.junit.jupiter.api.Assertions.*;

import io.avaje.jsonb.Jsonb;
import io.avaje.jsonb.Types;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SnapshotSerializationTest {
    private final Jsonb jsonb = Jsonb.builder().build();

    @Test
    void rawArrayListThrowsWithoutExplicitType() {
        var snapshots = new ArrayList<Map<String, Object>>();
        var entry = new HashMap<String, Object>();
        entry.put("snapshotName", "test.jpg");
        snapshots.add(entry);

        assertThrows(
                IllegalArgumentException.class, () -> jsonb.type(ArrayList.class).toJson(snapshots));
    }

    @SuppressWarnings("unchecked")
    @Test
    void imageSnapshotsSerializesWithExplicitType() {
        var snapshots = new ArrayList<Map<String, Object>>();
        var entry = new HashMap<String, Object>();
        entry.put("snapshotName", "test.jpg");
        entry.put("cameraUniqueName", "cam0");
        entry.put("snapshotData", new byte[] {1, 2, 3});
        entry.put("metadata", "{\"targets\":[]}");
        snapshots.add(entry);

        Type type = Types.listOf(Types.mapOf(Object.class));
        var adapter = (io.avaje.jsonb.JsonType<Object>) jsonb.type(type);
        String json = adapter.toJson(snapshots);

        assertNotNull(json);
        assertTrue(json.contains("snapshotName"));
        assertTrue(json.contains("test.jpg"));
        assertTrue(json.contains("metadata"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void calibSnapshotsSerializesWithExplicitType() {
        Map<String, Map<String, ArrayList<Map<String, Object>>>> snapshots = new HashMap<>();
        var camData = new HashMap<String, ArrayList<Map<String, Object>>>();
        var resolutionImages = new ArrayList<Map<String, Object>>();
        var entry = new HashMap<String, Object>();
        entry.put("snapshotData", new byte[] {1, 2, 3});
        entry.put("snapshotFilename", "img0.png");
        resolutionImages.add(entry);
        camData.put("1280x720", resolutionImages);
        snapshots.put("cam0", camData);

        Type type = Types.mapOf(Types.mapOf(Types.listOf(Types.mapOf(Object.class))));
        var adapter = (io.avaje.jsonb.JsonType<Object>) jsonb.type(type);
        String json = adapter.toJson(snapshots);

        assertNotNull(json);
        assertTrue(json.contains("snapshotFilename"));
        assertTrue(json.contains("img0.png"));
    }
}
