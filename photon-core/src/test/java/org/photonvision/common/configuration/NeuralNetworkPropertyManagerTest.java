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

package org.photonvision.common.configuration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.LinkedList;
import org.junit.jupiter.api.Test;
import org.photonvision.common.configuration.NeuralNetworkModelManager.Family;
import org.photonvision.common.configuration.NeuralNetworkModelManager.Version;
import org.photonvision.common.configuration.NeuralNetworkPropertyManager.ModelProperties;
import org.photonvision.common.util.file.JacksonUtils;

public class NeuralNetworkPropertyManagerTest {
    @Test
    void testSerialization() {
        var nnpm = new NeuralNetworkPropertyManager();
        // Path is always serialized as absolute; for the test to pass, this must also be made absolute
        nnpm.addModelProperties(
                new ModelProperties(
                        Path.of("test", "yolov8nCOCO.rknn").toAbsolutePath(),
                        "COCO",
                        new LinkedList<>(),
                        640,
                        640,
                        Family.RKNN,
                        Version.YOLOV8));
        String result = assertDoesNotThrow(() -> JacksonUtils.serializeToString(nnpm));
        var deserializedNnpm =
                assertDoesNotThrow(
                        () -> JacksonUtils.deserialize(result, NeuralNetworkPropertyManager.class));
        assertEquals(nnpm.getModels()[0], deserializedNnpm.getModels()[0]);
    }
}
