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

package org.photonvision.common.configuration;

import io.avaje.jsonb.JsonType;
import io.avaje.jsonb.Jsonb;
import io.avaje.jsonb.Types;
import java.util.Map;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

/**
 * Migrates legacy {@code AprilTagFieldLayout} JSON into the newer {@code org.wpilib.fields.Field}
 * format.
 *
 * <p>Older versions of PhotonVision stored the field layout with the top-level keys {@code field}
 * (a {@code {length, width}} object) and {@code tags} (a list of {@code {ID, pose}} objects). The
 * newer format instead uses {@code field-dimensions} and {@code field-tags}. The {@code pose}
 * objects themselves are identical between the two formats.
 */
public final class FieldLayoutMigration {
    private static final Logger logger = new Logger(FieldLayoutMigration.class, LogGroup.Config);

    private FieldLayoutMigration() {}

    /**
     * Converts legacy {@code AprilTagFieldLayout} JSON to the new {@code Field} JSON format.
     *
     * <p>Returns the input unchanged if it is already in the new format, cannot be recognized as a
     * field layout, or cannot be parsed. This way a {@code null} return is never used to signal
     * failure; callers that detect a migration simply persist the result and parse it.
     *
     * @param json The stored field layout JSON.
     * @return The migrated JSON, or the input unchanged if it did not need migrating.
     */
    public static String migrateFieldLayoutJson(String json) {
        try {
            final JsonType<Map<String, Object>> objMapJsonb =
                    Jsonb.instance().type(Types.mapOf(Object.class));

            Map<String, Object> layout = objMapJsonb.fromJson(json);
            if (layout == null
                    || layout.containsKey("field-dimensions")
                    || !layout.containsKey("field")) {
                // already the new format, or not a recognized field layout
                return json;
            }

            logger.info("Legacy AprilTagFieldLayout detected, migrating to Field format");
            layout.put("field-dimensions", layout.get("field"));
            if (layout.containsKey("tags")) {
                layout.put("field-tags", layout.get("tags"));
            }
            layout.remove("field");
            layout.remove("tags");

            return objMapJsonb.toJson(layout);
        } catch (RuntimeException e) {
            // unparseable JSON; leave it to the caller's own error handling
            return json;
        }
    }
}
