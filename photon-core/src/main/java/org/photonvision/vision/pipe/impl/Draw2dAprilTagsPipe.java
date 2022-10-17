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

package org.photonvision.vision.pipe.impl;

import java.awt.*;
import org.photonvision.vision.frame.FrameDivisor;

public class Draw2dAprilTagsPipe extends Draw2dTargetsPipe {
    public static class Draw2dAprilTagsParams extends Draw2dTargetsPipe.Draw2dTargetsParams {
        public Draw2dAprilTagsParams(
                boolean shouldDraw, boolean showMultipleTargets, FrameDivisor divisor) {
            super(shouldDraw, showMultipleTargets, divisor);
            // We want to show the polygon, not the rotated box
            this.showRotatedBox = false;
            this.showMaximumBox = false;
            this.rotatedBoxColor = Color.RED;
        }
    }
}
