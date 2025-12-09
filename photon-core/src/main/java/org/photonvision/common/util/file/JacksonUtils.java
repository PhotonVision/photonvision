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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ext.NioPathDeserializer;
import com.fasterxml.jackson.databind.ext.NioPathSerializer;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jetty.io.EofException;

public class JacksonUtils {
    public static class UIMap extends HashMap<String, Object> {}

    // Custom Path key deserializer for Maps with Path keys
    public static class PathKeySerializer
            extends com.fasterxml.jackson.databind.JsonSerializer<Path> {
        @Override
        public void serialize(Path value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            if (value == null) {
                gen.writeNull();
            } else {
                gen.writeFieldName(value.toUri().toString());
            }
        }
    }

    // Custom Path key deserializer for Maps with Path keys
    public static class PathKeyDeserializer extends com.fasterxml.jackson.databind.KeyDeserializer {
        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
            if (key == null || key.isEmpty()) {
                return null;
            }
            return Paths.get(URI.create(key));
        }
    }

    // Helper method to create ObjectMapper with Path serialization support
    private static ObjectMapper createObjectMapperWithPathSupport(Class<?> baseType) {
        PolymorphicTypeValidator ptv =
                BasicPolymorphicTypeValidator.builder().allowIfBaseType(baseType).build();

        SimpleModule pathModule = new SimpleModule();
        pathModule.addSerializer(Path.class, new NioPathSerializer());
        pathModule.addKeySerializer(Path.class, new PathKeySerializer());
        pathModule.addDeserializer(Path.class, new NioPathDeserializer());
        pathModule.addKeyDeserializer(Path.class, new PathKeyDeserializer());

        return JsonMapper.builder()
                .configure(JsonReadFeature.ALLOW_JAVA_COMMENTS, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT)
                .addModule(pathModule)
                .build();
    }

    public static <T> void serialize(Path path, T object) throws IOException {
        serialize(path, object, true);
    }

    public static <T> String serializeToString(T object) throws IOException {
        ObjectMapper objectMapper = createObjectMapperWithPathSupport(object.getClass());
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
    }

    public static <T> void serialize(Path path, T object, boolean forceSync) throws IOException {
        ObjectMapper objectMapper = createObjectMapperWithPathSupport(object.getClass());
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        saveJsonString(json, path, forceSync);
    }

    public static <T> T deserialize(Map<?, ?> s, Class<T> ref) throws IOException {
        ObjectMapper objectMapper = createObjectMapperWithPathSupport(ref);
        return objectMapper.convertValue(s, ref);
    }

    public static <T> T deserialize(String s, Class<T> ref) throws IOException {
        if (s.length() == 0) {
            throw new EofException("Provided empty string for class " + ref.getName());
        }

        ObjectMapper objectMapper = createObjectMapperWithPathSupport(ref);
        objectMapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);

        return objectMapper.readValue(s, ref);
    }

    public static <T> T deserialize(Path path, Class<T> ref) throws IOException {
        ObjectMapper objectMapper = createObjectMapperWithPathSupport(ref);
        File jsonFile = new File(path.toString());
        if (jsonFile.exists() && jsonFile.length() > 0) {
            return objectMapper.readValue(jsonFile, ref);
        }
        return null;
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
