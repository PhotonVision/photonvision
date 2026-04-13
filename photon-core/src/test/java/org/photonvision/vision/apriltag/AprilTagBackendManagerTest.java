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

package org.photonvision.vision.apriltag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class AprilTagBackendManagerTest {
    @AfterEach
    public void cleanup() {
        AprilTagBackendManager.resetForTest();
    }

    @Test
    public void noNvidiaJniFallsBackToCpu() {
        AprilTagBackendManager.setTestOverrides(true, false, true);

        var selection = AprilTagBackendManager.select(AprilTagFamily.kTag36h11);

        assertEquals(AprilTagDetectorBackend.CPU_WPILIB, selection.backend());
    }

    @Test
    public void tag16h5AlwaysFallsBackToCpu() {
        AprilTagBackendManager.setTestOverrides(true, true, true);

        var selection = AprilTagBackendManager.select(AprilTagFamily.kTag16h5);

        assertEquals(AprilTagDetectorBackend.CPU_WPILIB, selection.backend());
    }

    @Test
    public void forcedNvidiaWithoutSupportWarnsAndFallsBack() {
        System.setProperty("photonvision.apriltag.backend", "nvidia");
        AprilTagBackendManager.setTestOverrides(false, false, false);

        var selection = AprilTagBackendManager.select(AprilTagFamily.kTag36h11);

        assertEquals(AprilTagDetectorBackend.CPU_WPILIB, selection.backend());
        assertNotNull(selection.warning());
    }

    @Test
    public void forcedCpuOnNvidiaHostStaysOnCpu() {
        System.setProperty("photonvision.apriltag.backend", "cpu");
        AprilTagBackendManager.setTestOverrides(true, true, true);

        var selection = AprilTagBackendManager.select(AprilTagFamily.kTag36h11);

        assertEquals(AprilTagDetectorBackend.CPU_WPILIB, selection.backend());
    }
}
