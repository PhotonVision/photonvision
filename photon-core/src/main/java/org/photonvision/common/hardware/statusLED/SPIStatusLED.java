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
    protected LinearPattern pattern;

    protected PhotonStatus status = PhotonStatus.GENERIC_ERROR;

    public SPIStatusLED(
            NativeDeviceFactoryInterface deviceFactory,
            int spiBus,
            int chipSelect,
            int numLeds,
            int brightness) {
        ledChain = new Apa102LedDriver(spiBus, chipSelect, 2_000_000, numLeds, brightness);
        pattern = new LinearPattern(numLeds);

        TimedTaskManager.getInstance().addTask("StatusLEDUpdate", this::updateLED, 15);
    }

    @Override
    public void setStatus(PhotonStatus status) {
        this.status = status;
    }

    protected void updateLED() {
        for (pattern.pixel = 0; pattern.pixel < pattern.numPixels; pattern.pixel++) {
            switch (status) {
                case NT_CONNECTED_TARGETS_VISIBLE ->
                        ledChain.setPixelColour(pattern.pixel, PixelColour.BLUE);
                case NT_CONNECTED_TARGETS_MISSING ->
                        ledChain.setPixelColour(pattern.pixel, pattern.phaser(PixelColour.GREEN));
                case NT_DISCONNECTED_TARGETS_VISIBLE ->
                        ledChain.setPixelColour(pattern.pixel, pattern.throb(PixelColour.BLUE));
                case NT_DISCONNECTED_TARGETS_MISSING ->
                        ledChain.setPixelColour(pattern.pixel, pattern.throb(PixelColour.YELLOW));
                case GENERIC_ERROR ->
                        ledChain.setPixelColour(pattern.pixel, pattern.blink(PixelColour.RED));
            }
        }

        ledChain.render();
    }

    @Override
    public void close() throws Exception {
        ledChain.close();
    }
}
