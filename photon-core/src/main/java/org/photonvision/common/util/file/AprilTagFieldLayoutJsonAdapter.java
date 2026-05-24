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
import io.avaje.jsonb.CustomAdapter;
import io.avaje.jsonb.Json;
import io.avaje.jsonb.Jsonb;
import io.avaje.jsonb.Types;
import java.util.List;
import org.wpilib.vision.apriltag.AprilTag;
import org.wpilib.vision.apriltag.AprilTagFieldLayout;

@Json.Import(AprilTag.class)
@CustomAdapter
public class AprilTagFieldLayoutJsonAdapter implements JsonAdapter<AprilTagFieldLayout> {
    @Json
    record FieldDimensions(double length, double width) {}

    JsonAdapter<List<AprilTag>> aprilTagListJsonAdapter;
    JsonAdapter<FieldDimensions> fieldDimensionsJsonAdapter;
    PropertyNames names;

    public AprilTagFieldLayoutJsonAdapter(Jsonb jsonb) {
        aprilTagListJsonAdapter = jsonb.adapter(Types.listOf(AprilTag.class));
        fieldDimensionsJsonAdapter = jsonb.adapter(FieldDimensions.class);
        names = jsonb.properties("tags", "field");
    }

    @Override
    public void toJson(JsonWriter writer, AprilTagFieldLayout value) {
        writer.beginObject(names);
        writer.name(0);
        aprilTagListJsonAdapter.toJson(writer, value.getTags());
        writer.name(1);
        fieldDimensionsJsonAdapter.toJson(
                writer, new FieldDimensions(value.getFieldLength(), value.getFieldWidth()));
        writer.endObject();
    }

    @Override
    public AprilTagFieldLayout fromJson(JsonReader reader) {
        List<AprilTag> tags = null;
        FieldDimensions field = null;

        reader.beginObject();
        while (reader.hasNextField()) {
            final String fieldName = reader.nextField();
            switch (fieldName) {
                case "tags":
                    tags = aprilTagListJsonAdapter.fromJson(reader);
                    break;
                case "field":
                    field = fieldDimensionsJsonAdapter.fromJson(reader);
                    break;
                default:
                    reader.unmappedField(fieldName);
                    reader.skipValue();
            }
        }
        reader.endObject();

        return new AprilTagFieldLayout(tags, field.length, field.width);
    }
}
