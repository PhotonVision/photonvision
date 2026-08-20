package org.photonvision.common.configuration.migrations;

import io.avaje.json.JsonReader;
import io.avaje.json.JsonWriter;
import io.avaje.jsonb.Jsonb;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class DynamicJsonEditor implements AutoCloseable {
    private final Logger logger = new Logger(DynamicJsonEditor.class, LogGroup.Config);

    private final Jsonb jsonb;
    private Object rootNode;

    @Override
    public void close() {
        // No additional resources to release; the editor is a lightweight in-memory wrapper.
    }

    public DynamicJsonEditor(String initialJson) throws IOException {
        this.jsonb = Jsonb.builder().build();
        this.rootNode = parse(initialJson);
    }

    public Object getData() {
        return this.rootNode;
    }

    public void setData(Object rootNode) {
        this.rootNode = rootNode;
    }

    // --- TYPE-SAFE HELPER METHODS ---

    /** Resolves a value using dot-notation (e.g., "settings.theme" or "roles"). */
    public Object get(String path) {
        if (path == null || rootNode == null) {
            return null;
        } else if (path.isEmpty()) {
            return getData();
        }
        String[] keys = path.split("\\.");
        Object current = rootNode;

        for (String key : keys) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(key);
            } else {
                return null; // Path breaks if current element isn't a container object
            }
        }
        return current;
    }

    public boolean hasKey(String path) {
        if (path == null || path.isEmpty() || rootNode == null) {
            return false;
        }

        String[] keys = path.split("\\.");
        Object current = rootNode;

        for (String key : keys) {
            if (!(current instanceof Map)) {
                return false;
            }
            if (!((Map<?, ?>) current).containsKey(key)) {
                return false;
            }
            current = ((Map<?, ?>) current).get(key);
        }
        return true;
    }

    public String getString(String path) {
        Object val = get(path);
        return val != null ? String.valueOf(val) : null;
    }

    public Boolean getBoolean(String path) {
        Object val = get(path);
        if (val instanceof Boolean) return (Boolean) val;
        if (val instanceof String) return Boolean.parseBoolean((String) val);
        return null;
    }

    public Integer getInteger(String path) {
        Object val = get(path);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val instanceof String) return Integer.parseInt((String) val);
        return null;
    }

    public Double getDouble(String path) {
        Object val = get(path);
        if (val instanceof Number) return ((Number) val).doubleValue();
        if (val instanceof String) return Double.parseDouble((String) val);
        return null;
    }

    @SuppressWarnings("unchecked")
    public List<Object> getList(String path) {
        Object val = get(path);
        return val instanceof List ? (List<Object>) val : null;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String path, Class<T> type) {
        Object val = get(path);
        return val instanceof List ? (List<T>) val : null;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getMap(String path) {
        Object val = get(path);
        return val instanceof Map ? (Map<String, Object>) val : null;
    }

    // --- WRITER / EXPORT LOGIC ---

    public String export(boolean pretty) throws IOException {
        try (StringWriter stringWriter = new StringWriter();
                JsonWriter writer = jsonb.writer(stringWriter)) {
            writer.pretty(pretty);
            serializeValue(writer, this.rootNode);
            writer.flush();
            return stringWriter.toString();
        }
    }

    // --- INTERNAL PARSER / SERIALIZER LOGIC ---

    private Object parse(String json) throws IOException {
        if (json == null || json.strip().isEmpty()) return null;
        try (JsonReader reader = jsonb.reader(json)) {
            return deserializeToken(reader);
        }
    }

    private Object deserializeToken(JsonReader reader) throws IOException {
        JsonReader.Token tokenType = reader.currentToken();
        switch (tokenType) {
            case BEGIN_OBJECT:
                return parseObject(reader);
            case BEGIN_ARRAY:
                return parseArray(reader);
            case STRING:
                return reader.readString();
            case NUMBER:
                return parseNumber(reader);
            case BOOLEAN:
                return reader.readBoolean();
            case NULL:
                reader.skipValue();
                return null;
            default:
                reader.skipValue();
                return null;
        }
    }

    private Map<String, Object> parseObject(JsonReader reader) throws IOException {
        Map<String, Object> map = new LinkedHashMap<>();
        reader.beginObject();
        while (reader.hasNextField()) {
            map.put(reader.nextField(), deserializeToken(reader));
        }
        reader.endObject();
        return map;
    }

    private List<Object> parseArray(JsonReader reader) throws IOException {
        List<Object> list = new ArrayList<>();
        reader.beginArray();
        while (reader.hasNextElement()) {
            list.add(deserializeToken(reader));
        }
        reader.endArray();
        return list;
    }

    private Object parseNumber(JsonReader reader) throws IOException {
        String numberText = reader.readRaw().strip();
        if (numberText.contains(".") || numberText.contains("e") || numberText.contains("E")) {
            return Double.parseDouble(numberText);
        }
        try {
            return Integer.parseInt(numberText);
        } catch (NumberFormatException e) {
            try {
                return Long.parseLong(numberText);
            } catch (NumberFormatException ex) {
                return Double.parseDouble(numberText);
            }
        }
    }

    private void serializeValue(JsonWriter writer, Object value) throws IOException {
        if (value == null) {
            writer.nullValue();
        } else if (value instanceof Map) {
            serializeObject(writer, (Map<?, ?>) value);
        } else if (value instanceof List) {
            serializeArray(writer, (List<?>) value);
        } else if (value instanceof String) {
            writer.value((String) value);
        } else if (value instanceof Integer) {
            writer.value((Integer) value);
        } else if (value instanceof Double) {
            writer.value((Double) value);
        } else if (value instanceof Boolean) {
            writer.value((Boolean) value);
        } else {
            writer.value(value.toString());
            logger.debug("Type of " + value.toString() + " not known.");
        }
    }

    private void serializeObject(JsonWriter writer, Map<?, ?> map) throws IOException {
        writer.beginObject();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            writer.name(String.valueOf(entry.getKey()));
            serializeValue(writer, entry.getValue());
        }
        writer.endObject();
    }

    private void serializeArray(JsonWriter writer, List<?> list) throws IOException {
        writer.beginArray();
        for (Object item : list) {
            serializeValue(writer, item);
        }
        writer.endArray();
    }
}
