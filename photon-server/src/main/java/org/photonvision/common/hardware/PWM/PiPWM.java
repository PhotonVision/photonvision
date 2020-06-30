package org.photonvision.common.hardware.PWM;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.exception.UnsupportedPinModeException;
import com.pi4j.util.CommandArgumentParser;
import org.photonvision.common.hardware.PWM.PWMBase;

public class PiPWM extends PWMBase {
    private static final GpioController gpio = GpioFactory.getInstance();
    private final GpioPinPwmOutput pwm;
    private int pwmRange = 0;

    public PiPWM(int address) throws UnsupportedPinModeException {
        this.pwm =
                gpio.provisionPwmOutputPin(
                        CommandArgumentParser.getPin(RaspiPin.class, RaspiPin.getPinByAddress(address)));
    }

    @Override
    public void setPwmRate(int rate) {
        pwm.setPwm(rate);
    }

    @Override
    public void setPwmRange(int range) {
        pwm.setPwmRange(range);
        pwmRange = range;
    }

    @Override
    public int getPwmRate() {
        return pwm.getPwm();
    }

    @Override
    public int getPwmRange() {
        return pwmRange;
    }

    @Override
    public boolean shutdown() {
        gpio.shutdown();
        return gpio.isShutdown();
    }
}
