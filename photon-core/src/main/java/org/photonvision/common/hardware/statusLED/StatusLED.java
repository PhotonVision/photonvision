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
import java.util.List;
import java.util.function.Supplier;
import org.photonvision.common.hardware.PhotonStatus;

public interface StatusLED extends AutoCloseable {
    static final String pinErrorTemplate =
            "Expected %d pins for %s, but found %n pins; unassigned pins will be skipped, extra pins will be ignored";

    public void setStatus(PhotonStatus status);

    static StatusLED ofType(
            StatusLEDType type,
            Supplier<NativeDeviceFactoryInterface> lazyDeviceFactory,
            List<Integer> statusLedPins,
            boolean activeHigh) {
        return switch (type) {
            case RGB -> new RGBStatusLED(lazyDeviceFactory.get(), statusLedPins, activeHigh);
            case GreenYellow ->
                new GreenYellowStatusLED(lazyDeviceFactory.get(), statusLedPins, activeHigh);
        };
    }
}
