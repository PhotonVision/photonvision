package com.chameleonvision.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class JacksonHelper {
    private JacksonHelper() {} // no construction, utility class

    public static <T> void serializer(Path path, T object) throws IOException {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder().allowIfBaseType(object.getClass()).build();
        ObjectMapper objectMapper = JsonMapper.builder().activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT).build();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(path.toString()), object);
    }

    public static <T> T deserializer(Path path, Class<T> ref) throws IOException {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder().allowIfBaseType(ref).build();
        ObjectMapper objectMapper = JsonMapper.builder().activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT).build();
        File jsonFile = new File(path.toString());
        if (jsonFile.exists() && jsonFile.length() > 0) {
            return objectMapper.readValue(jsonFile, ref);
        }
        return null;
    }
}
