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
import org.wpilib.math.geometry.Pose3d;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.geometry.Translation3d;

@CustomAdapter
public class Pose3dJsonAdapter implements JsonAdapter<Pose3d>, ViewBuilderAware {
    private final JsonAdapter<Translation3d> translation3dJsonAdapter;
    private final JsonAdapter<Rotation3d> rotation3dJsonAdapter;
    private final PropertyNames names;

    public Pose3dJsonAdapter(Jsonb jsonb) {
        translation3dJsonAdapter = jsonb.adapter(Translation3d.class);
        rotation3dJsonAdapter = jsonb.adapter(Rotation3d.class);
        names = jsonb.properties("translation", "rotation");
    }

    @Override
    public void toJson(JsonWriter writer, Pose3d value) {
        writer.beginObject(names);
        writer.name(0);
        translation3dJsonAdapter.toJson(writer, value.getTranslation());
        writer.name(1);
        rotation3dJsonAdapter.toJson(writer, value.getRotation());
        writer.endObject();
    }

    @Override
    public Pose3d fromJson(JsonReader reader) {
        Translation3d translation = null;
        Rotation3d rotation = null;

        reader.beginObject(names);
        while (reader.hasNextField()) {
            final String fieldName = reader.nextField();
            switch (fieldName) {
                case "translation":
                    translation = translation3dJsonAdapter.fromJson(reader);
                    break;
                case "rotation":
                    rotation = rotation3dJsonAdapter.fromJson(reader);
                    break;
                default:
                    reader.unmappedField(fieldName);
                    reader.skipValue();
            }
        }
        reader.endObject();

        return new Pose3d(translation, rotation);
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
        builder.add(
                "translation",
                translation3dJsonAdapter,
                builder.method(Pose3d.class, "getTranslation", Translation3d.class));
        builder.add(
                "rotation",
                rotation3dJsonAdapter,
                builder.method(Pose3d.class, "getRotation", Rotation3d.class));
        builder.endObject();
    }
}
