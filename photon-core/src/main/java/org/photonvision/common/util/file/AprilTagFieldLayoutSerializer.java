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
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import org.wpilib.math.geometry.Pose3d;
import org.wpilib.math.geometry.Quaternion;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.geometry.Translation3d;
import org.wpilib.vision.apriltag.AprilTag;
import org.wpilib.vision.apriltag.AprilTagFieldLayout;

/**
 * Custom serializer for AprilTagFieldLayout to ensure consistent JSON format.
 */
public class AprilTagFieldLayoutSerializer extends JsonSerializer<AprilTagFieldLayout> {
    
    private void serializePose3d(Pose3d pose, JsonGenerator gen) throws IOException {
        gen.writeStartObject();
        
        // Serialize translation
        Translation3d translation = pose.getTranslation();
        gen.writeObjectFieldStart("translation");
        gen.writeNumberField("x", translation.getX());
        gen.writeNumberField("y", translation.getY());
        gen.writeNumberField("z", translation.getZ());
        gen.writeEndObject();
        
        // Serialize rotation
        Rotation3d rotation = pose.getRotation();
        Quaternion quaternion = rotation.getQuaternion();
        gen.writeObjectFieldStart("rotation");
        gen.writeObjectFieldStart("quaternion");
        gen.writeNumberField("X", quaternion.getX());
        gen.writeNumberField("Y", quaternion.getY());
        gen.writeNumberField("Z", quaternion.getZ());
        gen.writeNumberField("W", quaternion.getW());
        gen.writeEndObject();
        gen.writeEndObject();
        
        gen.writeEndObject();
    }
    
    @Override
    public void serialize(AprilTagFieldLayout value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        gen.writeStartObject();
        
        // Serialize field dimensions
        gen.writeObjectFieldStart("field");
        gen.writeNumberField("length", value.getFieldLength());
        gen.writeNumberField("width", value.getFieldWidth());
        gen.writeEndObject();
        
        // Serialize tags
        gen.writeArrayFieldStart("tags");
        for (AprilTag tag : value.getTags()) {
            gen.writeStartObject();
            gen.writeNumberField("ID", tag.ID);
            gen.writeFieldName("pose");
            serializePose3d(tag.pose, gen);
            gen.writeEndObject();
        }
        gen.writeEndArray();
        
        gen.writeEndObject();
    }
}


