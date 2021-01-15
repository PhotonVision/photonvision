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

import edu.wpi.first.networktables.EntryNotification;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import org.photonvision.common.hardware.GPIO.CustomGPIO;
import org.photonvision.common.hardware.GPIO.GPIOBase;
import org.photonvision.common.hardware.GPIO.pi.PigpioException;
import org.photonvision.common.hardware.GPIO.pi.PigpioPin;
import org.photonvision.common.hardware.GPIO.pi.PigpioSocket;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.TimedTaskManager;
import org.photonvision.common.util.math.MathUtils;

public class VisionLED {
    private static final Logger logger = new Logger(VisionLED.class, LogGroup.VisionModule);

    private final int[] ledPins;
    private final List<GPIOBase> visionLEDs = new ArrayList<>();
    private final int brightnessMin;
    private final int brightnessMax;
    private final PigpioSocket pigpioSocket;

    private VisionLEDMode currentLedMode = VisionLEDMode.VLM_DEFAULT;
    private BooleanSupplier pipelineModeSupplier;

    private int mappedBrightnessPercentage;

    public VisionLED(
            List<Integer> ledPins, int brightnessMin, int brightnessMax, PigpioSocket pigpioSocket) {
        this.brightnessMin = brightnessMin;
        this.brightnessMax = brightnessMax;
        this.pigpioSocket = pigpioSocket;
        this.ledPins = ledPins.stream().mapToInt(i -> i).toArray();
        ledPins.forEach(
                pin -> {
                    if (Platform.isRaspberryPi()) {
                        visionLEDs.add(new PigpioPin(pin));
                    } else {
                        visionLEDs.add(new CustomGPIO(pin));
                    }
                });
        pipelineModeSupplier = () -> false;
    }

    public void setPipelineModeSupplier(BooleanSupplier pipelineModeSupplier) {
        this.pipelineModeSupplier = pipelineModeSupplier;
    }

    public void setBrightness(int percentage) {
        mappedBrightnessPercentage = MathUtils.map(percentage, 0, 100, brightnessMin, brightnessMax);
        setInternal(currentLedMode, false);
    }

    public void blink(int pulseLengthMillis, int blinkCount) {
        blinkImpl(pulseLengthMillis, blinkCount);
        int blinkDuration = pulseLengthMillis * blinkCount * 2;
        TimedTaskManager.getInstance()
                .addOneShotTask(() -> setInternal(this.currentLedMode, false), blinkDuration + 150);
    }

    private void blinkImpl(int pulseLengthMillis, int blinkCount) {
        if (Platform.isRaspberryPi()) {
            try {
                setStateImpl(false); // hack to ensure hardware PWM has stopped before trying to blink
                pigpioSocket.generateAndSendWaveform(pulseLengthMillis, blinkCount, ledPins);
            } catch (PigpioException e) {
                logger.error("Failed to blink!", e);
            }
        } else {
            for (GPIOBase led : visionLEDs) {
                led.blink(pulseLengthMillis, blinkCount);
            }
        }
    }

    private void setStateImpl(boolean state) {
        if (Platform.isRaspberryPi()) {
            try {
                // stop any active blink
                pigpioSocket.waveTxStop();
            } catch (PigpioException e) {
                logger.error("Failed to stop blink!", e);
            }
        }
        // if the user has set an LED brightness other than 100%, use that instead
        if (mappedBrightnessPercentage == 100 || !state) {
            visionLEDs.forEach((led) -> led.setState(state));
        } else {
            visionLEDs.forEach((led) -> led.setBrightness(mappedBrightnessPercentage));
        }
    }

    public void setState(boolean on) {
        setInternal(on ? VisionLEDMode.VLM_ON : VisionLEDMode.VLM_OFF, false);
    }

    void onLedModeChange(EntryNotification entryNotification) {
        var newLedModeRaw = (int) entryNotification.value.getDouble();
        if (newLedModeRaw != currentLedMode.value) {
            VisionLEDMode newLedMode;
            switch (newLedModeRaw) {
                case -1:
                    newLedMode = VisionLEDMode.VLM_DEFAULT;
                    break;
                case 0:
                    newLedMode = VisionLEDMode.VLM_OFF;
                    break;
                case 1:
                    newLedMode = VisionLEDMode.VLM_ON;
                    break;
                case 2:
                    newLedMode = VisionLEDMode.VLM_BLINK;
                    break;
                default:
                    logger.warn("User supplied invalid LED mode, falling back to Default");
                    newLedMode = VisionLEDMode.VLM_DEFAULT;
                    break;
            }
            setInternal(newLedMode, true);
        }
    }

    private void setInternal(VisionLEDMode newLedMode, boolean fromNT) {
        var lastLedMode = currentLedMode;

        if (fromNT) {
            switch (newLedMode) {
                case VLM_DEFAULT:
                    setStateImpl(pipelineModeSupplier.getAsBoolean());
                    break;
                case VLM_OFF:
                    setStateImpl(false);
                    break;
                case VLM_ON:
                    setStateImpl(true);
                    break;
                case VLM_BLINK:
                    blinkImpl(85, -1);
                    break;
            }
            currentLedMode = newLedMode;
            logger.info(
                    "Changing LED mode from \""
                            + lastLedMode.toString()
                            + "\" to \""
                            + newLedMode.toString()
                            + "\"");
        } else {
            if (currentLedMode == VisionLEDMode.VLM_DEFAULT) {
                switch (newLedMode) {
                    case VLM_DEFAULT:
                        setStateImpl(pipelineModeSupplier.getAsBoolean());
                        break;
                    case VLM_OFF:
                        setStateImpl(false);
                        break;
                    case VLM_ON:
                        setStateImpl(true);
                        break;
                }
            }
            logger.info("Changing LED internal state to " + newLedMode.toString());
        }
    }

    public enum VisionLEDMode {
        VLM_DEFAULT(-1),
        VLM_OFF(0),
        VLM_ON(1),
        VLM_BLINK(2);

        public final int value;

        VisionLEDMode(int value) {
            this.value = value;
        }

        @Override
        public String toString() {
            switch (this) {
                case VLM_DEFAULT:
                    return "Default";
                case VLM_OFF:
                    return "Off";
                case VLM_ON:
                    return "On";
                case VLM_BLINK:
                    return "Blink";
            }
            return "";
        }
    }
}
