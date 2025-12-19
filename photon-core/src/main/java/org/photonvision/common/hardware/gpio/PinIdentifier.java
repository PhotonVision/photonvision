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

package org.photonvision.common.hardware.gpio;

import com.diozero.api.NoSuchDeviceException;
import com.diozero.api.PinInfo;
import com.diozero.internal.spi.NativeDeviceFactoryInterface;
import com.diozero.sbc.DeviceFactoryHelper;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
public interface PinIdentifier {
    public static final class NamedPin implements PinIdentifier {
        public final String name;

        protected NamedPin(String name) {
            this.name = name;
        }

        @Override
        public PinInfo info(NativeDeviceFactoryInterface deviceFactory) throws NoSuchDeviceException {
            PinInfo pinInfo = deviceFactory.getBoardPinInfo().getByName(name);
            if (pinInfo == null) {
                throw new NoSuchDeviceException("No such GPIO named \"" + name + "\"");
            }
            return pinInfo;
        }

        @JsonValue
        public String toValue() {
            return name;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof NamedPin) {
                return this.name == ((NamedPin) obj).name;
            } else {
                return super.equals(obj);
            }
        }
    }

    public static final class NumberedPin implements PinIdentifier {
        public final int number;

        protected NumberedPin(int number) {
            this.number = number;
        }

        @Override
        public PinInfo info(NativeDeviceFactoryInterface deviceFactory) throws NoSuchDeviceException {
            return deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(number);
        }

        @JsonValue
        public int toValue() {
            return number;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof NumberedPin) {
                return this.number == ((NumberedPin) obj).number;
            } else {
                return super.equals(obj);
            }
        }
    }

    public static PinIdentifier named(String name) {
        return new NamedPin(name);
    }

    public static PinIdentifier numbered(int number) {
        return new NumberedPin(number);
    }

    public default PinInfo info() throws NoSuchDeviceException {
        return info(DeviceFactoryHelper.getNativeDeviceFactory());
    }

    public PinInfo info(NativeDeviceFactoryInterface deviceFactory) throws NoSuchDeviceException;
}
