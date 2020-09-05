package org.photonvision.common.hardware;

import edu.wpi.first.networktables.EntryNotification;
import org.photonvision.common.hardware.GPIO.CustomGPIO;
import org.photonvision.common.hardware.GPIO.GPIOBase;
import org.photonvision.common.hardware.GPIO.PiGPIO;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public class VisionLED {
    private static final Logger logger = new Logger(VisionLED.class, LogGroup.VisionModule);

    public final List<GPIOBase> leds = new ArrayList<>();

    private VisionLEDMode currentLedMode = VisionLEDMode.VLM_DEFAULT;
    private BooleanSupplier pipelineModeSupplier;

    public VisionLED(List<Integer> ledPins, int pwmFreq, int pwmRangeMax) {
        ledPins.forEach(
                pin -> {
                    if (Platform.isRaspberryPi()) {
                        leds.add(new PiGPIO(pin, pwmFreq, pwmRangeMax));
                    } else {
                        leds.add(new CustomGPIO(pin));
                    }
                }
        );
        pipelineModeSupplier = () -> false;
    }

    public void setPipelineModeSupplier(BooleanSupplier pipelineModeSupplier) {
        this.pipelineModeSupplier = pipelineModeSupplier;
    }

    public void setBrightness(int percentage) {
        leds.forEach((led) -> led.setBrightness(percentage));
    }

    private void blinkImpl(int pulseLengthMillis, int blinkCount) {
        leds.forEach((led) -> led.blink(pulseLengthMillis, blinkCount));
    }

    private void setStateImpl(boolean state) {
        leds.forEach((led) -> led.setState(state));
    }

    public void setState(boolean on) {
        setInternal(on ? VisionLEDMode.VLM_ON : VisionLEDMode.VLM_OFF, false);
    }

    void onLedModeChange(EntryNotification entryNotification) {
        var newLedModeRaw = (int)entryNotification.value.getDouble();
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
                    blinkImpl(175, -1);
                    break;
            }
            currentLedMode = newLedMode;
            logger.info("Changing LED mode from \"" + lastLedMode.toString() + "\" to \"" + newLedMode.toString() + "\"");
        } else {
            if (currentLedMode == VisionLEDMode.VLM_DEFAULT) {
                switch (newLedMode) {
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
                case VLM_DEFAULT: return "Default";
                case VLM_OFF: return "Off";
                case VLM_ON: return "On";
                case VLM_BLINK: return "Blink";
            }
            return "";
        }
    }
}
