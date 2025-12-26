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

import com.diozero.api.DeviceMode;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.I2CConstants.AddressSize;
import com.diozero.api.PinInfo;
import com.diozero.api.RuntimeIOException;
import com.diozero.api.SerialConstants.DataBits;
import com.diozero.api.SerialConstants.Parity;
import com.diozero.api.SerialConstants.StopBits;
import com.diozero.api.SpiClockMode;
import com.diozero.internal.spi.AnalogInputDeviceInterface;
import com.diozero.internal.spi.AnalogOutputDeviceInterface;
import com.diozero.internal.spi.BaseNativeDeviceFactory;
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface;
import com.diozero.internal.spi.GpioDigitalInputOutputDeviceInterface;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;
import com.diozero.internal.spi.InternalI2CDeviceInterface;
import com.diozero.internal.spi.InternalPwmOutputDeviceInterface;
import com.diozero.internal.spi.InternalSerialDeviceInterface;
import com.diozero.internal.spi.InternalServoDeviceInterface;
import com.diozero.internal.spi.InternalSpiDeviceInterface;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class CustomDeviceFactory extends BaseNativeDeviceFactory {
    public static final String DEVICE_NAME = "Custom";

    public static final String GET_GPIO_PROP = "diozero.custom.getGPIO";
    public static final String SET_GPIO_PROP = "diozero.custom.setGPIO";
    public static final String SET_PWM_PROP = "diozero.custom.setPWM";
    public static final String SET_PWM_FREQUENCY_PROP = "diozero.custom.setPWMFrequency";
    public static final String RELEASE_GPIO_PROP = "diozero.custom.releaseGPIO";

    private static final Logger logger = new Logger(CustomDeviceFactory.class, LogGroup.General);

    public final CustomAdapter adapter;

    public CustomDeviceFactory(CustomAdapter adapter) {
        this.adapter = adapter;
    }

    public CustomDeviceFactory() {
        String getGPIOCommand = System.getProperty(GET_GPIO_PROP, "");
        String setGPIOCommand = System.getProperty(SET_GPIO_PROP, "");
        String setPWMCommand = System.getProperty(SET_PWM_PROP, "");
        String setPWMFrequencyCommand = System.getProperty(SET_PWM_FREQUENCY_PROP, "");
        String releaseGPIOCommand = System.getProperty(RELEASE_GPIO_PROP, "");

        this.adapter =
                new CustomAdapter(
                        getGPIOCommand,
                        setGPIOCommand,
                        setPWMCommand,
                        setPWMFrequencyCommand,
                        releaseGPIOCommand);
    }

    @Override
    public void start() {}

    @Override
    public void shutdown() {}

    @Override
    public String getName() {
        return DEVICE_NAME;
    }

    @Override
    public int getGpioValue(int gpio) {
        return adapter.getGPIO(gpio) ? 1 : 0;
    }

    @Override
    public DeviceMode getGpioMode(int gpio) {
        throw new UnsupportedOperationException("GPIO mode can not be retrieved");
    }

    @Override
    public int getBoardPwmFrequency() {
        throw new UnsupportedOperationException("PWM frequency cannot be retrieved");
    }

    @Override
    public void setBoardPwmFrequency(int pwmFrequency) {
        logger.warn("Setting PWM frequency is not implemented");
    }

    @Override
    public int getBoardServoFrequency() {
        throw new UnsupportedOperationException("Servo frequency cannot be retrieved");
    }

    @Override
    public void setBoardServoFrequency(int servoFrequency) {
        logger.warn("Setting servo frequency is not implemented");
    }

    @Override
    public GpioDigitalInputDeviceInterface createDigitalInputDevice(
            String key, PinInfo pinInfo, GpioPullUpDown pud, GpioEventTrigger trigger) {
        return new CustomDigitalInputDevice(this, key, pinInfo.getDeviceNumber(), pud, trigger);
    }

    @Override
    public GpioDigitalOutputDeviceInterface createDigitalOutputDevice(
            String key, PinInfo pinInfo, boolean initialValue) {
        return new CustomDigitalOutputDevice(this, key, pinInfo.getDeviceNumber(), initialValue);
    }

    @Override
    public GpioDigitalInputOutputDeviceInterface createDigitalInputOutputDevice(
            String key, PinInfo pinInfo, DeviceMode mode) {
        return new CustomDigitalInputOutputDevice(this, key, pinInfo.getDeviceNumber(), mode);
    }

    @Override
    public InternalPwmOutputDeviceInterface createPwmOutputDevice(
            String key, PinInfo pinInfo, int pwmFrequency, float initialValue) {
        return new CustomPwmOutputDevice(
                this, key, pinInfo.getDeviceNumber(), pwmFrequency, initialValue);
    }

    @Override
    public InternalServoDeviceInterface createServoDevice(
            String key,
            PinInfo pinInfo,
            int frequencyHz,
            int minPulseWidthUs,
            int maxPulseWidthUs,
            int initialPulseWidthUs) {
        throw new UnsupportedOperationException(
                "Servo devices are not supported by this device factory");
    }

    @Override
    public AnalogInputDeviceInterface createAnalogInputDevice(String key, PinInfo pinInfo) {
        throw new UnsupportedOperationException(
                "Analog inputs are not supported by this device factory");
    }

    @Override
    public AnalogOutputDeviceInterface createAnalogOutputDevice(
            String key, PinInfo pinInfo, float initialValue) {
        throw new UnsupportedOperationException(
                "Analog outputs are not supported by this device factory");
    }

    @Override
    public InternalSpiDeviceInterface createSpiDevice(
            String key,
            int controller,
            int chipSelect,
            int frequency,
            SpiClockMode spiClockMode,
            boolean lsbFirst)
            throws RuntimeIOException {
        throw new UnsupportedOperationException("SPI devices are not supported by this device factory");
    }

    @Override
    public InternalI2CDeviceInterface createI2CDevice(
            String key, int controller, int address, AddressSize addressSize) throws RuntimeIOException {
        throw new UnsupportedOperationException("I2C devices are not supported by this device factory");
    }

    @Override
    public InternalSerialDeviceInterface createSerialDevice(
            String key,
            String deviceFilename,
            int baud,
            DataBits dataBits,
            StopBits stopBits,
            Parity parity,
            boolean readBlocking,
            int minReadChars,
            int readTimeoutMillis)
            throws RuntimeIOException {
        throw new UnsupportedOperationException(
                "Serial communication is not supported by this device factory");
    }
}
