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

package org.photonvision.common.configuration;

import com.diozero.internal.spi.NativeDeviceFactoryInterface;
import io.avaje.jsonb.Json;
import org.photonvision.common.hardware.statusLED.GreenYellowStatusLED;
import org.photonvision.common.hardware.statusLED.RGBStatusLED;
import org.photonvision.common.hardware.statusLED.SPIStatusLED;
import org.photonvision.common.hardware.statusLED.StatusLED;

@Json(typeProperty = "type")
@Json.SubTypes({
    @Json.SubType(type = RGBStatusLED.Config.class, name = "RGB"),
    @Json.SubType(type = GreenYellowStatusLED.Config.class, name = "GreenYellow"),
    @Json.SubType(type = SPIStatusLED.Config.class, name = "SPI"),
})
public interface StatusLedConfig {
    public StatusLED create(NativeDeviceFactoryInterface deviceFactory);

    public int[] pins();

    @Override
    public String toString();
}
