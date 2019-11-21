package com.chameleonvision.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileHelper {
    private FileHelper() {} // no construction, utility class

    public static void CheckPath(String path) {
        if (path.equals("")) return;
        Path realPath = Path.of(path);
        CheckPath(realPath);
    }

    public static void CheckPath(Path path) {
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void Serializer(Path path, Object object) throws IOException {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class).build();
        ObjectMapper objectMapper = JsonMapper.builder().activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT).build();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(path.toString()), object);
    }

    public static <T> T DeSerializer(Path path, Class<T> ref) throws IOException {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder().allowIfBaseType(ref).build();
        ObjectMapper objectMapper = JsonMapper.builder().activateDefaultTyping(ptv).build();
        File jsonFile = new File(path.toString());
        if (jsonFile.exists() && jsonFile.length() > 0) {
            T readObject = objectMapper.readValue(jsonFile, ref);
            return readObject;
        }
        return null;
    }
}
