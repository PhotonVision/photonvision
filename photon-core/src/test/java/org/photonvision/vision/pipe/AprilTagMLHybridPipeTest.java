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

package org.photonvision.vision.pipe;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.photonvision.common.LoadJNI;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.pipe.impl.AprilTagMLHybridPipe;

/** Unit tests for {@link AprilTagMLHybridPipe} host-only behavior (no loaded NPU model). */
public class AprilTagMLHybridPipeTest {

    @BeforeAll
    public static void init() {
        LoadJNI.loadLibraries();
        ConfigManager.getInstance().load();
    }

    @Test
    public void testIsAvailableFalseWithoutModel() {
        AprilTagMLHybridPipe pipe = new AprilTagMLHybridPipe();
        assertFalse(pipe.isAvailable());

        var result = pipe.run(new Frame());
        assertNotNull(result.output);
        assertTrue(result.output.detections().isEmpty());
        assertTrue(result.output.rois().isEmpty());

        pipe.release();
    }

    @Test
    public void testReleaseForwardsToChildren() {
        AprilTagMLHybridPipe pipe = new AprilTagMLHybridPipe();
        pipe.release();
        pipe.release();
    }
}
