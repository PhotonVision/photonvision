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

package org.photonvision.common.hardware.GPIO.pi;

public class PigpioPulse {
    int gpioOn;
    int gpioOff;
    int delayMicros;

    /**
     * Initialises a pulse.
     *
     * @param gpioOn GPIO number to switch on at the start of the pulse. If zero, then no GPIO will be
     *     switched on.
     * @param gpioOff GPIO number to switch off at the start of the pulse. If zero, then no GPIO will
     *     be switched off.
     * @param delayMicros the delay in microseconds before the next pulse.
     */
    public PigpioPulse(int gpioOn, int gpioOff, int delayMicros) {
        this.gpioOn = gpioOn != 0 ? 1 << gpioOn : 0;
        this.gpioOff = gpioOff != 0 ? 1 << gpioOff : 0;
        this.delayMicros = delayMicros;
    }
}
