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

package org.photonvision.common.hardware;

import com.diozero.devices.LED;
import com.diozero.internal.spi.NativeDeviceFactoryInterface;
import java.util.List;
import org.photonvision.common.util.TimedTaskManager;

public class StatusLED implements AutoCloseable {
    public final LED redLED;
    public final LED greenLED;
    public final LED blueLED;
    protected int blinkCounter;

    protected PhotonStatus status = PhotonStatus.GENERIC_ERROR;

    public StatusLED(
            NativeDeviceFactoryInterface deviceFactory, List<Integer> statusLedPins, boolean activeHigh) {
        // fill unassigned pins with -1 to disable
        if (statusLedPins.size() != 3) {
            for (int i = 0; i < 3 - statusLedPins.size(); i++) {
                statusLedPins.add(-1);
            }
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
