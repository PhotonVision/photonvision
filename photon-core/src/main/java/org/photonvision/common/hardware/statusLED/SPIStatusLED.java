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
import org.photonvision.common.configuration.StatusLedConfig;
import org.photonvision.common.hardware.PhotonStatus;
import org.photonvision.common.util.TimedTaskManager;

public class SPIStatusLED implements StatusLED {
    public static class Config implements StatusLedConfig {
        public int spiBus = -1;
        public int chipSelect = -1;
        public int numLeds = 1;
        public int brightness = 0x04; // Brightness is in the range 0x00 to 0x1F

        @Override
        public StatusLED create(NativeDeviceFactoryInterface deviceFactory) {
            return new SPIStatusLED(deviceFactory, spiBus, chipSelect, numLeds, brightness);
        }

        @Override
        public int[] pins() {
            return new int[0];
        }

        @Override
        public String toString() {
            return "SPIStatusLED.Config[spiBus="
                    + spiBus
                    + ", chipSelect="
                    + chipSelect
                    + ", numLeds="
                    + numLeds
                    + ", brightness="
                    + brightness
                    + "]";
        }
    }

    Apa102LedDriver ledChain;
    protected int blinkCounter;

    protected PhotonStatus status = PhotonStatus.GENERIC_ERROR;

    public SPIStatusLED(
            NativeDeviceFactoryInterface deviceFactory,
            int spiBus,
            int chipSelect,
            int numLeds,
            int brightness) {
        ledChain = new Apa102LedDriver(spiBus, chipSelect, 2_000_000, numLeds, brightness);

        TimedTaskManager.getInstance().addTask("StatusLEDUpdate", this::updateLED, 150);
    }

    @Override
    public void setStatus(PhotonStatus status) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setStatus'");
    }

    protected void updateLED() {
        boolean blink = blinkCounter > 0;

        for (int pixel = 0; pixel < ledChain.getNumPixels(); pixel++) {
            switch (status) {
                case NT_CONNECTED_TARGETS_VISIBLE ->
                        // Blue
                        ledChain.setPixelColour(pixel, PixelColour.BLUE);
                case NT_CONNECTED_TARGETS_MISSING ->
                        // Blinking Green
                        ledChain.setPixelColour(pixel, blink ? PixelColour.GREEN : 0);
                case NT_DISCONNECTED_TARGETS_VISIBLE ->
                        // Blinking Blue
                        ledChain.setPixelColour(pixel, blink ? PixelColour.BLUE : 0);
                case NT_DISCONNECTED_TARGETS_MISSING ->
                        // Blinking Yellow
                        ledChain.setPixelColour(pixel, blink ? PixelColour.YELLOW : 0);
                case GENERIC_ERROR ->
                        // Blinking Red
                        ledChain.setPixelColour(pixel, blink ? PixelColour.RED : 0);
            }
        }

        blinkCounter++;
        blinkCounter %= 3;
    }

    @Override
    public void close() throws Exception {
        ledChain.close();
    }
}
