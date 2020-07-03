/*
 * Copyright (C) 2020 Photon Vision.
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

package org.photonvision.common.util;

/** A thread that tries to run at a specified loop time */
public abstract class LoopingRunnable implements Runnable {
    protected volatile Long loopTimeMs;

    protected abstract void process();

    public LoopingRunnable(Long loopTimeMs) {
        this.loopTimeMs = loopTimeMs;
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            var now = System.currentTimeMillis();

            // Do the thing
            process();

            // sleep for the remaining time
            var timeElapsed = System.currentTimeMillis() - now;
            var delta = loopTimeMs - timeElapsed;
            try {
                if (delta > 0.0) {

                    Thread.sleep(delta, 0);

                } else {
                    Thread.sleep(1);
                }
            } catch (Exception ignored) {
            }
        }
    }
}
