package org.photonvision.common.util;

import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

import java.util.HashMap;

public final class SerializationUtils {
    private static final Logger LOGGER = new Logger(SerializationUtils.class, LogGroup.General);

    public static HashMap<String, Object> objectToHashMap(Object src) {
        var ret = new HashMap<String, Object>();
        for(var field : src.getClass().getFields()) {
            try {
                if (!field.getType().isEnum()) { // if the field is not an enum, get it based on the current pipeline
                    ret.put(field.getName(), field.get(src));
                } else {
                    var ordinal = (Enum) field.get(src);
                    ret.put(field.getName(), ordinal.ordinal());
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                LOGGER.error("Could not serialize " + src.getClass().getSimpleName());
                e.printStackTrace();
            }
        }
        return ret;
    }
}
