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

package org.photonvision.common.hardware.statusLED;

import com.diozero.devices.LED;
import com.diozero.internal.spi.NativeDeviceFactoryInterface;
import java.util.Collections;
import java.util.List;
import org.photonvision.common.hardware.PhotonStatus;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.TimedTaskManager;

/** Basic RGB LED with individual control over each pin */
public class RGBStatusLED implements StatusLED {
    private final Logger logger = new Logger(RGBStatusLED.class, LogGroup.General);

    public final LED redLED;
    public final LED greenLED;
    public final LED blueLED;
    protected int blinkCounter;

    protected PhotonStatus status = PhotonStatus.GENERIC_ERROR;

    public RGBStatusLED(
            NativeDeviceFactoryInterface deviceFactory, List<Integer> statusLedPins, boolean activeHigh) {
        if (statusLedPins.size() != 3) {
            logger.warn(pinErrorTemplate.formatted(3, "a RGB status LED", statusLedPins.size()));
        }
        // fill unassigned pins with -1 to disable
        if (statusLedPins.size() < 3) {
            statusLedPins.addAll(Collections.nCopies(statusLedPins.size() - 3, -1));
        }

        // Outputs are active-low for a common-anode RGB LED
        redLED = new LED(deviceFactory, statusLedPins.get(0), activeHigh, false);
        greenLED = new LED(deviceFactory, statusLedPins.get(1), activeHigh, false);
        blueLED = new LED(deviceFactory, statusLedPins.get(2), activeHigh, false);

        TimedTaskManager.getInstance().addTask("StatusLEDUpdate", this::updateLED, 150);
    }

    protected void setRGB(boolean r, boolean g, boolean b) {
        redLED.setOn(r);
        greenLED.setOn(g);
        blueLED.setOn(b);
    }

    @Override
    public void setStatus(PhotonStatus status) {
        this.status = status;
    }

    protected void updateLED() {
        boolean blink = blinkCounter > 0;

        switch (status) {
            case NT_CONNECTED_TARGETS_VISIBLE ->
                    // Blue
                    setRGB(false, false, true);
            case NT_CONNECTED_TARGETS_MISSING ->
                    // Blinking Green
                    setRGB(false, blink, false);
            case NT_DISCONNECTED_TARGETS_VISIBLE ->
                    // Blinking Blue
                    setRGB(false, false, blink);
            case NT_DISCONNECTED_TARGETS_MISSING ->
                    // Blinking Yellow
                    setRGB(blink, blink, false);
            case GENERIC_ERROR ->
                    // Blinking Red
                    setRGB(blink, false, false);
        }

        blinkCounter++;
        blinkCounter %= 3;
    }

    @Override
    public void close() {
        redLED.close();
        greenLED.close();
        blueLED.close();
    }
}
