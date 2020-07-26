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

package org.photonvision.common.hardware.PWM;

import eu.xeli.jpigpio.PigpioException;
import eu.xeli.jpigpio.Pulse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class PiPWM extends PWMBase {

    private static final Logger logger = new Logger(PWMBase.class, LogGroup.General);
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ArrayList<Pulse> pulses = new ArrayList<>();
    private final int pin;

    public PiPWM(int pin, int value, int range) {
        this.pin = pin;
        try {
            pigpio.setPWMFrequency(this.pin, value);
            pigpio.setPWMRange(this.pin, range);
        } catch (PigpioException e) {
            logger.error("Could not set PWM settings on port " + this.pin);
            e.printStackTrace();
        }
    }

    @Override
    public void setPwmRange(List<Integer> range) {
        try {
            pigpio.setPWMRange(this.pin, range.get(0));
        } catch (PigpioException e) {
            logger.error("Could not set PWM range on port " + this.pin);
            e.printStackTrace();
        }
    }

    @Override
    public List<Integer> getPwmRange() {
        try {
            return List.of(0, pigpio.getPWMRange(this.pin));
        } catch (PigpioException e) {
            logger.error("Could not get PWM range on port " + this.pin);
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean shutdown() {
        try {
            pigpio.gpioTerminate();
        } catch (PigpioException e) {
            logger.error("Could not stop GPIO instance");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void blink(int pulseTimeMillis, int blinks) {
        try {
            pulses.clear();
            for (int i = 0; i < blinks; i++) {
                pulses.add(new Pulse(1 << this.pin, 0, pulseTimeMillis * 100));
                pulses.add(new Pulse(0, 1 << this.pin, pulseTimeMillis * 100));
            }
            pigpio.waveAddGeneric(this.pulses);
            pigpio.waveSendOnce(pigpio.waveCreate());
        } catch (PigpioException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void dimLED(int dimPercentage) {
        try {
            pigpio.setPWMDutycycle(this.pin, getPwmRange().get(1) * (dimPercentage / 100));
        } catch (PigpioException e) {
            logger.error("Could not dim PWM on port " + this.pin);
            e.printStackTrace();
        }
    }
}
