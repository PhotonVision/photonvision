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
import org.jetbrains.annotations.Nullable;
import org.photonvision.common.hardware.PhotonStatus;

public interface StatusLED extends AutoCloseable {
    public void setStatus(PhotonStatus status);

    @Nullable
    static StatusLED ofType(
            StatusLEDType type,
            Supplier<NativeDeviceFactoryInterface> lazyDeviceFactory,
            List<Integer> statusLedPins,
            boolean activeHigh) {
        return switch (type) {
            case RGB ->
                    statusLedPins.size() == 3
                            ? new RGBStatusLED(lazyDeviceFactory.get(), statusLedPins, activeHigh)
                            : null;
            case GY ->
                    statusLedPins.size() == 2
                            ? new GYStatusLED(lazyDeviceFactory.get(), statusLedPins, activeHigh)
                            : null;
        };
    }
}
