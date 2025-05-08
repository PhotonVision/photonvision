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

package org.photonvision.vision.pipeline;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedList;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.photonvision.common.configuration.NeuralNetworkModelManager;

public class ObjectDetectionTest {
    private static LinkedList<String[]> passNames =
            new LinkedList<String[]>(
                    java.util.Arrays.asList(
                            new String[] {"note-640-640-yolov5s.rknn", "note-640-640-yolov5s-labels.txt"},
                            new String[] {"object-640-640-yolov8n.rknn", "object-640-640-yolov8n-labels.txt"},
                            new String[] {
                                "example_1.2-640-640-yolov5l.rknn", "example_1.2-640-640-yolov5l-labels.txt"
                            },
                            new String[] {"demo_3.5-640-640-yolov8m.rknn", "demo_3.5-640-640-yolov8m-labels.txt"},
                            new String[] {"sample-640-640-yolov5x.rknn", "sample-640-640-yolov5x-labels.txt"},
                            new String[] {
                                "test_case-640-640-yolov8s.rknn", "test_case-640-640-yolov8s-labels.txt"
                            },
                            new String[] {
                                "model_ABC-640-640-yolov5n.rknn", "model_ABC-640-640-yolov5n-labels.txt"
                            },
                            new String[] {"my_model-640-640-yolov8x.rknn", "my_model-640-640-yolov8x-labels.txt"},
                            new String[] {"name_1.0-640-640-yolov5n.rknn", "name_1.0-640-640-yolov5n-labels.txt"},
                            new String[] {
                                "valid_name-640-640-yolov8s.rknn", "valid_name-640-640-yolov8s-labels.txt"
                            },
                            new String[] {
                                "test.model-640-640-yolov5l.rknn", "test.model-640-640-yolov5l-labels.txt"
                            },
                            new String[] {
                                "case1_test-640-640-yolov8m.rknn", "case1_test-640-640-yolov8m-labels.txt"
                            },
                            new String[] {"A123-640-640-yolov5x.rknn", "A123-640-640-yolov5x-labels.txt"},
                            new String[] {
                                "z_y_test.model-640-640-yolov8n.rknn", "z_y_test.model-640-640-yolov8n-labels.txt"
                            }));
    private static LinkedList<String[]> parsedPassNames =
            new LinkedList<String[]>(
                    java.util.Arrays.asList(
                            new String[] {"note", "640", "640", "yolov5s"},
                            new String[] {"object", "640", "640", "yolov8n"},
                            new String[] {"example_1.2", "640", "640", "yolov5l"},
                            new String[] {"demo_3.5", "640", "640", "yolov8m"},
                            new String[] {"sample", "640", "640", "yolov5x"},
                            new String[] {"test_case", "640", "640", "yolov8s"},
                            new String[] {"model_ABC", "640", "640", "yolov5n"},
                            new String[] {"my_model", "640", "640", "yolov8x"},
                            new String[] {"name_1.0", "640", "640", "yolov5n"},
                            new String[] {"valid_name", "640", "640", "yolov8s"},
                            new String[] {"test.model", "640", "640", "yolov5l"},
                            new String[] {"case1_test", "640", "640", "yolov8m"},
                            new String[] {"A123", "640", "640", "yolov5x"},
                            new String[] {"z_y_test.model", "640", "640", "yolov8n"}));
    private static LinkedList<String[]> failNames =
            new LinkedList<String[]>(
                    java.util.Arrays.asList(
                            new String[] {"note-yolov5s.rknn", "note-640-640-yolov5s-labels.txt"},
                            new String[] {"640-640-yolov8n.rknn", "object-640-640-yolov8n-labels.txt"},
                            new String[] {"example_1.2.rknn", "example_1.2-640-640-yolov5l-labels.txt"},
                            new String[] {"demo_3.5-640-yolov8m.rknn", "demo_3.5-640-640-yolov8m-labels.txt"},
                            new String[] {"sample-640.rknn", "sample-640-640-yolov5x-labels.txt"},
                            new String[] {"test_case.txt", "test_case-640-640-yolov8s-labels.txt"},
                            new String[] {"model_ABC.onnx", "model_ABC-640-640-yolov5n-labels.txt"},
                            new String[] {"my_model", "my_model-640-640-yolov8x-labels.txt"},
                            new String[] {"name_1.0-yolov5n.rknn", "wrong-labels.txt"},
                            new String[] {"", "valid_name-640-640-yolov8s-labels.txt"},
                            new String[] {null, "test.model-640-640-yolov5l-labels.txt"},
                            new String[] {"case1_test-640-640-yolov8m.rknn", null},
                            new String[] {"A123-640-640.rknn", "different-labels.txt"},
                            new String[] {"z_y_test.model", ""}));

    // Test the model name validation for names that ought to pass
    @ParameterizedTest
    @MethodSource("verifyPassNameProvider")
    public void testRKNNVerificationPass(String[] names) {
        NeuralNetworkModelManager.verifyRKNNNames(names[0], names[1]);
    }

    // // Test the model name validation for names that ought to fail
    @ParameterizedTest
    @MethodSource("verifyFailNameProvider")
    public void testRNNVerificationFail(String[] names) {
        assertThrows(
                IllegalArgumentException.class,
                () -> NeuralNetworkModelManager.verifyRKNNNames(names[0], names[1]));
    }

    // Test the model name parsing
    @ParameterizedTest
    @MethodSource("parseNameProvider")
    public void testRKNNNameParsing(String[] expected, String name) {
        String[] parsed = NeuralNetworkModelManager.parseRKNNName(name);
        assertArrayEquals(expected, parsed);
    }

    static Stream<Arguments> verifyPassNameProvider() {
        return passNames.stream().map(array -> Arguments.of((Object) array));
    }

    static Stream<Arguments> verifyFailNameProvider() {
        return failNames.stream().map(array -> Arguments.of((Object) array));
    }

    static Stream<Arguments> parseNameProvider() {
        // return a stream of parsed pass names, and the first element of each pass name
        return passNames.stream()
                .map(name -> Arguments.of(parsedPassNames.get(passNames.indexOf(name)), name[0]));
    }
}
