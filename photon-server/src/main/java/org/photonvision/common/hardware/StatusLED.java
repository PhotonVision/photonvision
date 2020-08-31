package org.photonvision.common.hardware;

import org.photonvision.common.hardware.GPIO.CustomGPIO;
import org.photonvision.common.hardware.GPIO.GPIOBase;
import org.photonvision.common.hardware.GPIO.PiGPIO;

import java.util.List;

public class StatusLED {
    public final GPIOBase redLED;
    public final GPIOBase greenLED;
    public final GPIOBase blueLED;

    public StatusLED(List<Integer> statusLedPins) {
        // fill unassigned pins with -1 to disable
        if (statusLedPins.size() != 3) {
            for (int i = 0; i < 3 - statusLedPins.size(); i++) {
                statusLedPins.add(-1);
            }
        }

        if (Platform.isRaspberryPi()) {
            redLED = new PiGPIO(statusLedPins.get(0));
            greenLED = new PiGPIO(statusLedPins.get(1));
            blueLED = new PiGPIO(statusLedPins.get(2));
        } else {
            redLED = new CustomGPIO(statusLedPins.get(0));
            greenLED = new CustomGPIO(statusLedPins.get(1));
            blueLED = new CustomGPIO(statusLedPins.get(2));
        }
    }
}
