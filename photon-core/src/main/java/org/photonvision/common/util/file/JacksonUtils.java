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

package org.photonvision.common.util.file;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;

public class JacksonUtils {
    public static class UIMap extends HashMap<String, Object> {}

    public static <T> void serialize(Path path, T object) throws IOException {
        serialize(path, object, true);
    }

    public static <T> void serialize(Path path, T object, boolean forceSync) throws IOException {
        PolymorphicTypeValidator ptv =
                BasicPolymorphicTypeValidator.builder().allowIfBaseType(object.getClass()).build();
        ObjectMapper objectMapper =
                JsonMapper.builder()
                        .activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT)
                        .build();
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        saveJsonString(json, path, forceSync);
    }

    public static <T> T deserialize(Path path, Class<T> ref) throws IOException {
        PolymorphicTypeValidator ptv =
                BasicPolymorphicTypeValidator.builder().allowIfBaseType(ref).build();
        ObjectMapper objectMapper =
                JsonMapper.builder()
                        .configure(JsonReadFeature.ALLOW_JAVA_COMMENTS, true)
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT)
                        .build();
        File jsonFile = new File(path.toString());
        if (jsonFile.exists() && jsonFile.length() > 0) {
            return objectMapper.readValue(jsonFile, ref);
        }
        return null;
    }

    public static <T> T deserialize(Path path, Class<T> ref, StdDeserializer<T> deserializer)
            throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(ref, deserializer);
        objectMapper.registerModule(module);

        File jsonFile = new File(path.toString());
        if (jsonFile.exists() && jsonFile.length() > 0) {
            return objectMapper.readValue(jsonFile, ref);
        }
        return null;
    }

    public static <T> void serialize(Path path, T object, Class<T> ref, StdSerializer<T> serializer)
            throws IOException {
        serialize(path, object, ref, serializer, true);
    }

    public static <T> void serialize(
            Path path, T object, Class<T> ref, StdSerializer<T> serializer, boolean forceSync)
            throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(ref, serializer);
        objectMapper.registerModule(module);
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        saveJsonString(json, path, forceSync);
    }

    private static void saveJsonString(String json, Path path, boolean forceSync) throws IOException {
        var file = path.toFile();
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            if (!file.canWrite()) {
                file.setWritable(true);
            }
            file.createNewFile();
        }
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write(json.getBytes());
        fileOutputStream.flush();
        if (forceSync) {
            FileDescriptor fileDescriptor = fileOutputStream.getFD();
            fileDescriptor.sync();
        }
        fileOutputStream.close();
    }
}
