package org.photonvision.common.util.file;

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

public class JacksonUtils {
    public static <T> void serializer(Path path, T object) throws IOException {
        serializer(path, object, false);
    }

    public static <T> void serializer(Path path, T object, boolean forceSync) throws IOException {
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
        serialize(path, object, ref, serializer, false);
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
        FileOutputStream fileOutputStream = new FileOutputStream(path.toFile());
        fileOutputStream.write(json.getBytes());
        fileOutputStream.flush();
        if (forceSync) {
            FileDescriptor fileDescriptor = fileOutputStream.getFD();
            fileDescriptor.sync();
        }
        fileOutputStream.close();
    }
}
