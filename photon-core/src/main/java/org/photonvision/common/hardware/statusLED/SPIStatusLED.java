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

import com.diozero.internal.spi.NativeDeviceFactoryInterface;
import com.diozero.ws281xj.PixelColour;
import com.diozero.ws281xj.apa102.Apa102LedDriver;
import java.util.List;
import org.photonvision.common.hardware.PhotonStatus;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.TimedTaskManager;

public class SPIStatusLED implements StatusLED {
    private final Logger logger = new Logger(SPIStatusLED.class, LogGroup.General);

    Apa102LedDriver statusLed;
    protected int blinkCounter;

    protected PhotonStatus status = PhotonStatus.GENERIC_ERROR;

    public SPIStatusLED(NativeDeviceFactoryInterface deviceFactory, List<Integer> statusLedPins) {
        if (statusLedPins.size() != 2) {
            logger.error(
                    strictPinErrorTemplate.formatted(
                            2, "a SPI (DotStar/APA102/SK9822) status LED", statusLedPins.size()));
        }

        statusLed = new Apa102LedDriver(statusLedPins.get(0), statusLedPins.get(1), 2_000_000, 1, 0x1F);

        TimedTaskManager.getInstance().addTask("StatusLEDUpdate", this::updateLED, 150);
    }

    @Override
    public void setStatus(PhotonStatus status) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setStatus'");
    }

    protected void updateLED() {
        boolean blink = blinkCounter > 0;

        switch (status) {
            case NT_CONNECTED_TARGETS_VISIBLE ->
                    // Blue
                    statusLed.setPixelColour(0, PixelColour.BLUE);
            case NT_CONNECTED_TARGETS_MISSING ->
                    // Blinking Green
                    statusLed.setPixelColour(0, blink ? PixelColour.GREEN : 0);
            case NT_DISCONNECTED_TARGETS_VISIBLE ->
                    // Blinking Blue
                    statusLed.setPixelColour(0, blink ? PixelColour.BLUE : 0);
            case NT_DISCONNECTED_TARGETS_MISSING ->
                    // Blinking Yellow
                    statusLed.setPixelColour(0, blink ? PixelColour.YELLOW : 0);
            case GENERIC_ERROR ->
                    // Blinking Red
                    statusLed.setPixelColour(0, blink ? PixelColour.RED : 0);
        }

        blinkCounter++;
        blinkCounter %= 3;
    }

    @Override
    public void close() throws Exception {
        statusLed.close();
    }
}
