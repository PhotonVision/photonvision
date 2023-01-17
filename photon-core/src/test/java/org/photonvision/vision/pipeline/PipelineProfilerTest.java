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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PipelineProfilerTest {
    @Test
    public void reflectiveProfile() {
        long[] invalidNanos = new long[20];
        long[] validNanos = new long[PipelineProfiler.ReflectivePipeCount];

        for (int i = 0; i < validNanos.length; i++) {
            validNanos[i] = (long) (i * 1e+6); // fill data
        }

        var invalidResult = PipelineProfiler.getReflectiveProfileString(invalidNanos);
        var validResult = PipelineProfiler.getReflectiveProfileString(validNanos);

        System.out.println(validResult);

        Assertions.assertEquals("Invalid data", invalidResult);
        Assertions.assertTrue(validResult.contains("Total: 45.0ms"));
    }
}
