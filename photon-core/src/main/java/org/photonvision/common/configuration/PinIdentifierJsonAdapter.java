package org.photonvision.common.configuration;

import io.avaje.json.JsonAdapter;
import io.avaje.json.JsonReader;
import io.avaje.json.JsonWriter;
import io.avaje.jsonb.CustomAdapter;
import io.avaje.jsonb.Jsonb;

@CustomAdapter
public class PinIdentifierJsonAdapter implements JsonAdapter<PinIdentifier> {

    private final JsonAdapter<String> stringJsonAdapter;
    private final JsonAdapter<Integer> intJsonAdapter;
    private final JsonAdapter<Object> genericJsonAdapter;

    public PinIdentifierJsonAdapter(Jsonb jsonb) {
        stringJsonAdapter = jsonb.adapter(String.class);
        intJsonAdapter = jsonb.adapter(int.class);
        genericJsonAdapter = jsonb.adapter(Object.class);
    }

    @Override
    public void toJson(JsonWriter writer, PinIdentifier value) {
        genericJsonAdapter.toJson(writer, value);
    }

    @Override
    public PinIdentifier fromJson(JsonReader reader) {
        switch (reader.currentToken()) {
            case NUMBER:
                return PinIdentifier.numbered(intJsonAdapter.fromJson(reader));
            case STRING:
                return PinIdentifier.named(stringJsonAdapter.fromJson(reader));
            default:
                throw new IllegalStateException(
                        String.format("Unable to parse %s as a PinIdentifier", reader.currentToken()));
        }
    }
}
