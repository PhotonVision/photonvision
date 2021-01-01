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

import java.util.HashMap;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public final class SerializationUtils {
    private static final Logger logger = new Logger(SerializationUtils.class, LogGroup.General);

    public static HashMap<String, Object> objectToHashMap(Object src) {
        var ret = new HashMap<String, Object>();
        for (var field : src.getClass().getFields()) {
            try {
                field.setAccessible(true);
                if (!field
                        .getType()
                        .isEnum()) { // if the field is not an enum, get it based on the current pipeline
                    ret.put(field.getName(), field.get(src));
                } else {
                    var ordinal = (Enum) field.get(src);
                    ret.put(field.getName(), ordinal.ordinal());
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                logger.error("Could not serialize " + src.getClass().getSimpleName(), e);
            }
        }
        return ret;
    }
}
