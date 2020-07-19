package org.photonvision.vision.pipeline;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PipelineProfilerTest {

    @Test
    public void reflectiveProfile() {
        long[] invalidNanos = new long[10];
        long[] validNanos = new long[PipelineProfiler.ReflectivePipeCount];

        for (int i = 0; i < validNanos.length; i++) {
            validNanos[i] = (long) (i * 1e+6); // fill data
        }

        var invalidResult = PipelineProfiler.getReflectiveProfileString(invalidNanos);
        var validResult = PipelineProfiler.getReflectiveProfileString(validNanos);

        Assertions.assertEquals("Invalid data", invalidResult);
        Assertions.assertTrue(validResult.contains("Total: 171.0ms"));
        System.out.println(validResult);
    }
}
