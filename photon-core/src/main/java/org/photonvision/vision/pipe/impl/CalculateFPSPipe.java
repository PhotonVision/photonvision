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

import edu.wpi.first.math.filter.LinearFilter;
import edu.wpi.first.wpilibj.Timer;
import org.photonvision.vision.pipe.CVPipe;

public class CalculateFPSPipe
        extends CVPipe<Void, Integer, CalculateFPSPipe.CalculateFPSPipeParams> {
    private final LinearFilter fpsFilter = LinearFilter.movingAverage(20);

    // roll my own Timer, since this is so trivial
    double lastTime = -1;

    @Override
    protected Integer process(Void in) {
        if (lastTime < 0) {
            lastTime = Timer.getFPGATimestamp();
        }

        var now = Timer.getFPGATimestamp();
        var dtSeconds = now - lastTime;
        lastTime = now;

        // If < 1 uS between ticks, something is probably wrong
        int fps;
        if (dtSeconds < 1e-6) {
            fps = 0;
        } else {
            fps = (int) fpsFilter.calculate(1 / dtSeconds);
        }

        return fps;
    }

    public static class CalculateFPSPipeParams {}
}
