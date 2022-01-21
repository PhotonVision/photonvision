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
import org.apache.commons.lang3.time.StopWatch;
import org.photonvision.vision.pipe.CVPipe;

public class CalculateFPSPipe
        extends CVPipe<Void, Integer, CalculateFPSPipe.CalculateFPSPipeParams> {
    private LinearFilter fpsFilter = LinearFilter.movingAverage(20);
    StopWatch clock = new StopWatch();

    @Override
    protected Integer process(Void in) {
        if (!clock.isStarted()) {
            clock.reset();
            clock.start();
        }
        clock.stop();
        var fps = (int) fpsFilter.calculate(1000.0 / clock.getTime());
        clock.reset();
        clock.start();
        return fps;
    }

    public static class CalculateFPSPipeParams {}
}
