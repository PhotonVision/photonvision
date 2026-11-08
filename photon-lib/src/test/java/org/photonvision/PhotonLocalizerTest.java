/*
 * MIT License
 *
 * Copyright (c) PhotonVision
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.photonvision;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.photonvision.estimation.TargetModel;
import org.photonvision.jni.LibraryLoader;
import org.wpilib.fields.Field;
import org.wpilib.fields.Fields;
import org.wpilib.hardware.hal.HAL;
import org.wpilib.util.runtime.RuntimeLoader;

public class PhotonLocalizerTest {
    static Field reefscape2025;
    static TargetModel tagModel;
    static PhotonLocalizer localizer;

    @BeforeAll
    public static void init() throws IOException {
        if (!LibraryLoader.loadWpiLibraries()) {
            fail();
        }
        RuntimeLoader.loadLibrary("photontargetingJNI");

        HAL.initialize();

        reefscape2025 = Field.loadField(Fields.FRC_2025_REEFSCAPE_WELDED);
        tagModel = TargetModel.kAprilTag36h11;
    }

    @AfterAll
    public static void teardown() {
        HAL.shutdown();
    }

    @Test
    void testLocalizerCreation() {
        assertDoesNotThrow(() -> new PhotonLocalizer(reefscape2025, tagModel));
    }

    @Test
    void testLocalizerStartStop() {
        localizer = new PhotonLocalizer(reefscape2025, tagModel);
        assertDoesNotThrow(() -> localizer.start());
        assertDoesNotThrow(() -> localizer.stop());
    }
}
