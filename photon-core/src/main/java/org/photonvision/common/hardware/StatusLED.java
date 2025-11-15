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
import com.diozero.sbc.DeviceFactoryHelper;
import java.util.List;

public class StatusLED implements AutoCloseable {
    public final LED redLED;
    public final LED greenLED;
    public final LED blueLED;

    public StatusLED(List<Integer> statusLedPins, boolean activeHigh) {
        this(DeviceFactoryHelper.getNativeDeviceFactory(), statusLedPins, activeHigh);
    }

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
    }

    public void setRGB(boolean r, boolean g, boolean b) {
        redLED.setOn(r);
        greenLED.setOn(g);
        blueLED.setOn(b);
    }

    @Override
    public void close() {
        redLED.close();
        greenLED.close();
        blueLED.close();
    }
}
