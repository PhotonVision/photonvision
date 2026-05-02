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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.wpilib.math.geometry.Pose3d;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.geometry.Translation3d;
import org.wpilib.vision.apriltag.AprilTag;
import org.wpilib.vision.apriltag.AprilTagFieldLayout;

/**
 * Unit tests for AprilTagFieldLayout serialization and deserialization.
 */
public class AprilTagFieldLayoutSerializationTest {

    @Test
    public void testSerializeAndDeserializeEmptyFieldLayout() throws IOException {
        // Create an empty field layout
        AprilTagFieldLayout original = new AprilTagFieldLayout(new ArrayList<>(), 10.0, 20.0);

        // Serialize to JSON string
        String json = JacksonUtils.serializeToString(original);
        assertNotNull(json);
        assertTrue(json.length() > 0);

        // Deserialize back from JSON string
        AprilTagFieldLayout deserialized = JacksonUtils.deserialize(json, AprilTagFieldLayout.class);
        assertNotNull(deserialized);
    }

    @Test
    public void testSerializeAndDeserializeWithTags() throws IOException {
        // Create a field layout with some tags
        List<AprilTag> tags = new ArrayList<>();
        tags.add(new AprilTag(1, new Pose3d(1.0, 2.0, 3.0, new Rotation3d())));
        tags.add(new AprilTag(2, new Pose3d(4.0, 5.0, 6.0, new Rotation3d())));
        tags.add(
                new AprilTag(
                        3, new Pose3d(7.0, 8.0, 9.0, new Rotation3d(0.1, 0.2, 0.3))));

        AprilTagFieldLayout original = new AprilTagFieldLayout(tags, 52.5, 26.5);

        // Serialize to JSON string
        String json = JacksonUtils.serializeToString(original);
        assertNotNull(json);
        assertTrue(json.length() > 0);

        // Deserialize back from JSON string
        AprilTagFieldLayout deserialized = JacksonUtils.deserialize(json, AprilTagFieldLayout.class);
        assertNotNull(deserialized);

        // Verify the deserialized layout has the same tags
        List<AprilTag> deserializedTags = deserialized.getTags();
        assertEquals(3, deserializedTags.size());
        assertEquals(1, deserializedTags.get(0).ID);
        assertEquals(2, deserializedTags.get(1).ID);
        assertEquals(3, deserializedTags.get(2).ID);
    }

    @Test
    public void testSerializationPreservesFieldDimensions() throws IOException {
        double fieldLength = 54.0;
        double fieldWidth = 27.0;
        List<AprilTag> tags = new ArrayList<>();
        tags.add(new AprilTag(1, new Pose3d(1.0, 2.0, 3.0, new Rotation3d())));

        AprilTagFieldLayout original = new AprilTagFieldLayout(tags, fieldLength, fieldWidth);

        // Serialize and deserialize
        String json = JacksonUtils.serializeToString(original);
        AprilTagFieldLayout deserialized = JacksonUtils.deserialize(json, AprilTagFieldLayout.class);

        // Verify field dimensions are preserved
        assertEquals(fieldLength, deserialized.getFieldLength(), 0.0001);
        assertEquals(fieldWidth, deserialized.getFieldWidth(), 0.0001);
    }

    @Test
    public void testSerializationPreservesTagPoses() throws IOException {
        List<AprilTag> tags = new ArrayList<>();
        Pose3d pose = new Pose3d(1.5, 2.5, 3.5, new Rotation3d(0.1, 0.2, 0.3));
        tags.add(new AprilTag(42, pose));

        AprilTagFieldLayout original = new AprilTagFieldLayout(tags, 50.0, 25.0);

        // Serialize and deserialize
        String json = JacksonUtils.serializeToString(original);
        AprilTagFieldLayout deserialized = JacksonUtils.deserialize(json, AprilTagFieldLayout.class);

        // Verify tag pose is preserved
        List<AprilTag> deserializedTags = deserialized.getTags();
        assertEquals(1, deserializedTags.size());
        assertEquals(42, deserializedTags.get(0).ID);
        Pose3d deserializedPose = deserializedTags.get(0).pose;
        assertEquals(pose.getX(), deserializedPose.getX(), 0.0001);
        assertEquals(pose.getY(), deserializedPose.getY(), 0.0001);
        assertEquals(pose.getZ(), deserializedPose.getZ(), 0.0001);
    }

    @Test
    public void testJsonFormat() throws IOException {
        List<AprilTag> tags = new ArrayList<>();
        tags.add(new AprilTag(1, new Pose3d(1.0, 0.0, 0.0, new Rotation3d())));

        AprilTagFieldLayout layout = new AprilTagFieldLayout(tags, 10.0, 20.0);

        // Serialize to JSON string
        String json = JacksonUtils.serializeToString(layout);

        // Verify JSON contains expected fields
        assertTrue(json.contains("\"field\""), "JSON should contain 'field' object");
        assertTrue(json.contains("\"length\""), "JSON should contain 'length'");
        assertTrue(json.contains("\"width\""), "JSON should contain 'width'");
        assertTrue(json.contains("\"tags\""), "JSON should contain 'tags' array");
    }

    @Test
    public void testRoundTripWithMultipleTags() throws IOException {
        // Create a field layout with multiple tags at various positions
        List<AprilTag> originalTags = new ArrayList<>();
        for (int i = 1; i <= 16; i++) {
            double x = i * 0.5;
            double y = i * 1.0;
            double z = i * 0.1;
            Pose3d pose = new Pose3d(x, y, z, new Rotation3d());
            originalTags.add(new AprilTag(i, pose));
        }

        AprilTagFieldLayout original = new AprilTagFieldLayout(originalTags, 54.0, 27.0);

        // Serialize and deserialize
        String json = JacksonUtils.serializeToString(original);
        AprilTagFieldLayout deserialized = JacksonUtils.deserialize(json, AprilTagFieldLayout.class);

        // Verify all tags are preserved
        List<AprilTag> deserializedTags = deserialized.getTags();
        assertEquals(originalTags.size(), deserializedTags.size());

        for (int i = 0; i < originalTags.size(); i++) {
            AprilTag originalTag = originalTags.get(i);
            AprilTag deserializedTag = deserializedTags.get(i);
            assertEquals(originalTag.ID, deserializedTag.ID);
            assertEquals(originalTag.pose.getX(), deserializedTag.pose.getX(), 0.0001);
            assertEquals(originalTag.pose.getY(), deserializedTag.pose.getY(), 0.0001);
            assertEquals(originalTag.pose.getZ(), deserializedTag.pose.getZ(), 0.0001);
        }
    }
}
