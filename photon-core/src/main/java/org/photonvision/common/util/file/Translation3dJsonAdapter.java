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

import io.avaje.json.JsonAdapter;
import io.avaje.json.JsonReader;
import io.avaje.json.JsonWriter;
import io.avaje.json.PropertyNames;
import io.avaje.json.view.ViewBuilder;
import io.avaje.json.view.ViewBuilderAware;
import io.avaje.jsonb.CustomAdapter;
import io.avaje.jsonb.Jsonb;
import java.lang.invoke.MethodHandle;
import org.wpilib.math.geometry.Translation3d;

@CustomAdapter
public class Translation3dJsonAdapter implements JsonAdapter<Translation3d>, ViewBuilderAware {
    private final JsonAdapter<Double> doubleJsonAdapter;
    private final PropertyNames names;

    public Translation3dJsonAdapter(Jsonb jsonb) {
        doubleJsonAdapter = jsonb.adapter(Double.class);
        names = jsonb.properties("x", "y", "z");
    }

    @Override
    public void toJson(JsonWriter writer, Translation3d value) {
        writer.beginObject(names);
        writer.name(0);
        doubleJsonAdapter.toJson(writer, value.getX());
        writer.name(1);
        doubleJsonAdapter.toJson(writer, value.getY());
        writer.name(2);
        doubleJsonAdapter.toJson(writer, value.getZ());
        writer.endObject();
    }

    @Override
    public Translation3d fromJson(JsonReader reader) {
        double x = 0;
        double y = 0;
        double z = 0;

        reader.beginObject(names);
        while (reader.hasNextField()) {
            final String fieldName = reader.nextField();
            switch (fieldName) {
                case "x":
                    x = doubleJsonAdapter.fromJson(reader);
                    break;
                case "y":
                    y = doubleJsonAdapter.fromJson(reader);
                    break;
                case "z":
                    z = doubleJsonAdapter.fromJson(reader);
                    break;
                default:
                    reader.unmappedField(fieldName);
                    reader.skipValue();
            }
        }
        reader.endObject();

        return new Translation3d(x, y, z);
    }

    @Override
    public boolean isViewBuilderAware() {
        return true;
    }

    @Override
    public ViewBuilderAware viewBuild() {
        return this;
    }

    @Override
    public void build(ViewBuilder builder, String name, MethodHandle handle) {
        builder.beginObject(name, handle);
        builder.add("x", doubleJsonAdapter, builder.method(Translation3d.class, "getX", Double.class));
        builder.add("y", doubleJsonAdapter, builder.method(Translation3d.class, "getY", Double.class));
        builder.add("z", doubleJsonAdapter, builder.method(Translation3d.class, "getZ", Double.class));
        builder.endObject();
    }
}
