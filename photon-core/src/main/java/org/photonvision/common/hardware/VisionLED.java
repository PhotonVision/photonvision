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
import com.diozero.devices.PwmLed;
import com.diozero.internal.spi.NativeDeviceFactoryInterface;
import com.diozero.sbc.BoardPinInfo;
import com.diozero.sbc.DeviceFactoryHelper;
import edu.wpi.first.networktables.NetworkTableEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.TimedTaskManager;
import org.photonvision.common.util.math.MathUtils;

public class VisionLED implements AutoCloseable {
    private static final Logger logger = new Logger(VisionLED.class, LogGroup.VisionModule);

    private final List<LED> visionLEDs = new ArrayList<>();
    private final List<PwmLed> dimmableVisionLEDs = new ArrayList<>();
    private final int brightnessMin;
    private final int brightnessMax;

    private VisionLEDMode currentLedMode = VisionLEDMode.kDefault;
    private BooleanSupplier pipelineModeSupplier;

    private float mappedBrightness = 0.0f;

    private final Consumer<Integer> modeConsumer;

    public VisionLED(
            List<Integer> ledPins,
            boolean ledsCanDim,
            int brightnessMin,
            int brightnessMax,
            int pwmFrequency,
            Consumer<Integer> visionLEDmode) {
        this(
                DeviceFactoryHelper.getNativeDeviceFactory(),
                ledPins,
                ledsCanDim,
                brightnessMin,
                brightnessMax,
                pwmFrequency,
                visionLEDmode);
    }

    public VisionLED(
            NativeDeviceFactoryInterface deviceFactory,
            List<Integer> ledPins,
            boolean ledsCanDim,
            int brightnessMin,
            int brightnessMax,
            int pwmFrequency,
            Consumer<Integer> visionLEDmode) {
        this.brightnessMin = brightnessMin;
        this.brightnessMax = brightnessMax;
        this.modeConsumer = visionLEDmode;
        if (pwmFrequency > 0) {
            deviceFactory.setBoardPwmFrequency(pwmFrequency);
        }
        BoardPinInfo boardPinInfo = deviceFactory.getBoardPinInfo();
        ledPins.forEach(
                pin -> {
                    if (ledsCanDim && boardPinInfo.getByPwmOrGpioNumberOrThrow(pin).isPwmOutputSupported()) {
                        PwmLed led = new PwmLed(deviceFactory, pin);
                        if (pwmFrequency > 0) {
                            led.setPwmFrequency(pwmFrequency);
                        }
                        dimmableVisionLEDs.add(led);
                    } else {
                        visionLEDs.add(new LED(deviceFactory, pin));
                    }
                });
        pipelineModeSupplier = () -> false;
    }

    public void setPipelineModeSupplier(BooleanSupplier pipelineModeSupplier) {
        this.pipelineModeSupplier = pipelineModeSupplier;
    }

    public void setBrightness(int percentage) {
        mappedBrightness =
                (float) (MathUtils.map(percentage, 0.0, 100.0, brightnessMin, brightnessMax) / 100.0);
        setInternal(currentLedMode, false);
    }

    public void blink(int pulseLengthMillis, int blinkCount) {
        blinkImpl(pulseLengthMillis, blinkCount);
        int blinkDuration = pulseLengthMillis * blinkCount * 2;
        TimedTaskManager.getInstance()
                .addOneShotTask(() -> setInternal(this.currentLedMode, false), blinkDuration + 150);
    }

    private void blinkImpl(int pulseLengthMillis, int blinkCount) {
        for (LED led : visionLEDs) {
            led.blink(pulseLengthMillis, pulseLengthMillis, blinkCount, true);
        }
        for (PwmLed led : dimmableVisionLEDs) {
            led.blink(pulseLengthMillis, pulseLengthMillis, blinkCount, true);
        }
    }

    private void setStateImpl(boolean state) {
        for (LED led : visionLEDs) {
            led.setOn(state);
        }
        for (PwmLed led : dimmableVisionLEDs) {
            led.setValue(mappedBrightness);
        }
    }

    public void setState(boolean on) {
        setInternal(on ? VisionLEDMode.kOn : VisionLEDMode.kOff, false);
    }

    void onLedModeChange(NetworkTableEvent entryNotification) {
        var newLedModeRaw = (int) entryNotification.valueData.value.getInteger();
        logger.debug("Got LED mode " + newLedModeRaw);
        if (newLedModeRaw != currentLedMode.value) {
            VisionLEDMode newLedMode =
                    switch (newLedModeRaw) {
                        case -1 -> VisionLEDMode.kDefault;
                        case 0 -> VisionLEDMode.kOff;
                        case 1 -> VisionLEDMode.kOn;
                        case 2 -> VisionLEDMode.kBlink;
                        default -> {
                            logger.warn("User supplied invalid LED mode, falling back to Default");
                            yield VisionLEDMode.kDefault;
                        }
                    };
            setInternal(newLedMode, true);

            if (modeConsumer != null) modeConsumer.accept(newLedMode.value);
        }
    }

    private void setInternal(VisionLEDMode newLedMode, boolean fromNT) {
        var lastLedMode = currentLedMode;

        if (fromNT || currentLedMode == VisionLEDMode.kDefault) {
            switch (newLedMode) {
                case kDefault -> setStateImpl(pipelineModeSupplier.getAsBoolean());
                case kOff -> setStateImpl(false);
                case kOn -> setStateImpl(true);
                case kBlink -> blinkImpl(85, -1);
            }
        }

        if (fromNT) {
            currentLedMode = newLedMode;
            logger.info(
                    "Changing LED mode from \"" + lastLedMode.toString() + "\" to \"" + newLedMode + "\"");
        } else {
            logger.info("Changing LED internal state to " + newLedMode.toString());
        }
    }

    @Override
    public void close() {
        for (LED led : visionLEDs) {
            led.close();
        }
        for (PwmLed led : dimmableVisionLEDs) {
            led.close();
        }
    }
}
