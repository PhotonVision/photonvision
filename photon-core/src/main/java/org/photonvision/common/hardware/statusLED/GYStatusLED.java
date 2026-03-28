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
import java.util.List;
import org.photonvision.common.hardware.PhotonStatus;
import org.photonvision.common.util.TimedTaskManager;

/** A pair of green and yellow LEDs, as used on the Limelight cameras */
public class GYStatusLED implements StatusLED {
    public final LED greenLED;
    public final LED yellowLED;
    protected int blinkCounter;

    protected PhotonStatus status = PhotonStatus.GENERIC_ERROR;

    public GYStatusLED(
            NativeDeviceFactoryInterface deviceFactory, List<Integer> statusLedPins, boolean activeHigh) {
        // fill unassigned pins with -1 to disable
        if (statusLedPins.size() != 3) {
            for (int i = 0; i < 3 - statusLedPins.size(); i++) {
                statusLedPins.add(-1);
            }
        }

        // Outputs are active-low for a common-anode RGB LED
        greenLED = new LED(deviceFactory, statusLedPins.get(0), activeHigh, false);
        yellowLED = new LED(deviceFactory, statusLedPins.get(1), activeHigh, false);

        TimedTaskManager.getInstance().addTask("StatusLEDUpdate", this::updateLED, 75);
    }

    protected void setLEDs(boolean green, boolean yellow) {
        greenLED.setOn(green);
        yellowLED.setOn(yellow);
    }

    @Override
    public void setStatus(PhotonStatus status) {
        this.status = status;
    }

    protected void updateLED() {
        boolean slowBlink = blinkCounter > 1;
        boolean fastBlink = (blinkCounter % 2) > 0;

        switch (status) {
            case NT_CONNECTED_TARGETS_VISIBLE ->
                    // Green fast, yellow on
                    setLEDs(fastBlink, true);
            case NT_CONNECTED_TARGETS_MISSING ->
                    // Green slow, yellow on
                    setLEDs(slowBlink, true);
            case NT_DISCONNECTED_TARGETS_VISIBLE ->
                    // Green fast, yellow slow
                    setLEDs(fastBlink, slowBlink);
            case NT_DISCONNECTED_TARGETS_MISSING ->
                    // Green slow, yellow slow
                    setLEDs(slowBlink, slowBlink);
            case GENERIC_ERROR ->
                    // No lights
                    setLEDs(false, false);
        }

        blinkCounter++;
        blinkCounter %= 6;
    }

    @Override
    public void close() throws Exception {
        greenLED.close();
        yellowLED.close();
    }
}
