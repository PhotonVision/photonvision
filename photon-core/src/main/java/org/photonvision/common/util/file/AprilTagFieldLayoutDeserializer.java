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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.wpilib.math.geometry.Pose3d;
import org.wpilib.math.geometry.Quaternion;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.geometry.Translation3d;
import org.wpilib.vision.apriltag.AprilTag;
import org.wpilib.vision.apriltag.AprilTagFieldLayout;

/**
 * Custom deserializer for AprilTagFieldLayout to handle proper conversion from JSON.
 */
public class AprilTagFieldLayoutDeserializer extends JsonDeserializer<AprilTagFieldLayout> {
    
    private Pose3d deserializePose3d(JsonNode poseNode) {
        JsonNode translationNode = poseNode.get("translation");
        double x = translationNode.get("x").asDouble();
        double y = translationNode.get("y").asDouble();
        double z = translationNode.get("z").asDouble();
        Translation3d translation = new Translation3d(x, y, z);
        
        JsonNode rotationNode = poseNode.get("rotation");
        JsonNode quaternionNode = rotationNode.get("quaternion");
        double qX = quaternionNode.get("X").asDouble();
        double qY = quaternionNode.get("Y").asDouble();
        double qZ = quaternionNode.get("Z").asDouble();
        double qW = quaternionNode.get("W").asDouble();
        Quaternion quaternion = new Quaternion(qW, qX, qY, qZ);
        Rotation3d rotation = new Rotation3d(quaternion);
        
        return new Pose3d(translation, rotation);
    }
    
    @Override
    public AprilTagFieldLayout deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        // Extract field dimensions
        JsonNode fieldNode = node.get("field");
        double length = fieldNode.get("length").asDouble();
        double width = fieldNode.get("width").asDouble();

        // Extract tags
        List<AprilTag> tags = new ArrayList<>();
        JsonNode tagsNode = node.get("tags");
        if (tagsNode != null && tagsNode.isArray()) {
            for (JsonNode tagNode : tagsNode) {
                int id = tagNode.get("ID").asInt();
                JsonNode poseNode = tagNode.get("pose");
                Pose3d pose = deserializePose3d(poseNode);
                tags.add(new AprilTag(id, pose));
            }
        }

        return new AprilTagFieldLayout(tags, length, width);
    }
}

