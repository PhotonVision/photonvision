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

package org.photonvision.model.manager;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedList;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Base test class for model manager implementations. Provides common test utilities and methods.
 */
public abstract class ModelManagerTest {
    /**
     * Get the model manager implementation to test.
     *
     * @return The model manager instance.
     */
    protected abstract ModelManager getModelManager();

    /**
     * Get the list of valid name pairs for testing.
     *
     * @return A list of valid model/label name pairs.
     */
    protected abstract LinkedList<String[]> getValidNamePairs();

    /**
     * Get the list of invalid name pairs for testing.
     *
     * @return A list of invalid model/label name pairs.
     */
    protected abstract LinkedList<String[]> getInvalidNamePairs();

    /**
     * Get the list of parsed model info for valid model names.
     *
     * @return A list of parsed model info (baseName, width, height, versionString).
     */
    protected abstract LinkedList<String[]> getParsedValidNames();

    /** Test the model name validation for names that ought to pass. */
    @ParameterizedTest
    @MethodSource("verifyPassNameProvider")
    public void testVerificationPass(String[] names) {
        getModelManager().verifyNames(names[0], names[1]);
    }

    /** Test the model name validation for names that ought to fail. */
    @ParameterizedTest
    @MethodSource("verifyFailNameProvider")
    public void testVerificationFail(String[] names) {
        assertThrows(
                IllegalArgumentException.class, () -> getModelManager().verifyNames(names[0], names[1]));
    }

    /** Test the model name parsing. */
    @ParameterizedTest
    @MethodSource("parseNameProvider")
    public void testNameParsing(String[] expected, String name) {
        var parsedModelInfo = getModelManager().parseModelName(name);
        String[] parsed = {
            parsedModelInfo.baseName,
            String.valueOf(parsedModelInfo.width),
            String.valueOf(parsedModelInfo.height),
            parsedModelInfo.versionString
        };
        assertArrayEquals(expected, parsed);
    }

    /** Provides the valid name pairs for testing name validation. */
    static Stream<Arguments> verifyPassNameProvider() {
        return new LinkedList<String[]>().stream().map(array -> Arguments.of((Object) array));
    }

    /** Provides the invalid name pairs for testing name validation failures. */
    static Stream<Arguments> verifyFailNameProvider() {
        return new LinkedList<String[]>().stream().map(array -> Arguments.of((Object) array));
    }

    /** Provides the test cases for name parsing verification. */
    static Stream<Arguments> parseNameProvider() {
        return Stream.empty();
    }
}
