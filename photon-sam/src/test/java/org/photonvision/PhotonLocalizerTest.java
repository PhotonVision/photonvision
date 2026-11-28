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
        RuntimeLoader.loadLibrary("photonsamJNI");

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
