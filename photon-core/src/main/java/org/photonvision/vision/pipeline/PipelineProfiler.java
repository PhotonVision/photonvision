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

import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.math.MathUtils;

public class PipelineProfiler {
    private static boolean shouldLog;

    private static final Logger reflectiveLogger =
            new Logger(ReflectivePipeline.class, LogGroup.VisionModule);
    private static final Logger coloredShapeLogger =
            new Logger(ColoredShapePipeline.class, LogGroup.VisionModule);

    /**
     * Indices for Reflective profiling 0 - rotateImagePipe 1 - inputCopy (not a pipe) 2 - hsvPipe 3 -
     * findContoursPipe 4 - speckleRejectPipe 5 - filterContoursPipe 6 - groupContoursPipe 7 -
     * sortContoursPipe 8 - collect2dTargetsPipe 9 - cornerDetectionPipe 10 - solvePNPPipe (OPTIONAL)
     * 11 - outputMatPipe (OPTIONAL) 12 - draw2dCrosshairPipe (on input) 13 - draw2dCrosshairPipe (on
     * output) 14 - draw2dTargetsPipe (on input) 15 - draw2dTargetsPipe (on output) 16 -
     * draw3dTargetsPipe (OPTIONAL, on input) 17 - draw3dTargetsPipe (OPTIONAL, on output)
     */
    private static final String[] ReflectivePipeNames =
            new String[] {
                "RotateImage",
                "HSV",
                "FindContours",
                "SpeckleReject",
                "FilterContours",
                "GroupContours",
                "SortContours",
                "Collect2dTargets",
                "CornerDetection",
                "SolvePNP",
            };

    public static final int ReflectivePipeCount = ReflectivePipeNames.length;

    protected static String getReflectiveProfileString(long[] nanos) {
        if (nanos.length != ReflectivePipeCount) {
            return "Invalid data";
        }

        var sb = new StringBuilder("Profiling - ");
        double totalMs = 0;
        for (int i = 0; i < nanos.length; i++) {
            if ((i == 10 || i == 11 || i == 17 || i == 18) && nanos[i] == 0) {
                continue; // skip empty pipe profiles
            }

            sb.append(ReflectivePipeNames[i]);
            sb.append(": ");
            var ms = MathUtils.roundTo(nanos[i] / 1e+6, 3);
            totalMs += ms;
            sb.append(ms);
            sb.append("ms");
            //            boolean isLast = (i + 1 == 17 && nanos[i+1] == 0) || i == 18;
            //            if (isLast) {
            //                var foo = "bar";
            //            } else {
            sb.append(", ");
            //            }
        }

        sb.append("Total: ");
        sb.append(MathUtils.roundTo(totalMs, 3));
        sb.append("ms");

        return sb.toString();
    }

    public static void printReflectiveProfile(long[] nanos) {
        if (shouldLog) {
            reflectiveLogger.trace(() -> getReflectiveProfileString(nanos));
        }
    }

    public static void enablePrint(boolean enable) {
        shouldLog = enable;
    }
}
