package org.photonvision.common.hardware.GPIO.pi;

public class PigpioPulse {
    int gpioOn;
    int gpioOff;
    int delayMicros;

    /**
     * Initialises a pulse.
     *
     * @param gpioOn
     * GPIO number to switch on at the start of the pulse.
     * If zero, then no GPIO will be switched on.
     * @param gpioOff
     * GPIO number to switch off at the start of the pulse.
     * If zero, then no GPIO will be switched off.
     * @param delayMicros the delay in microseconds before the next pulse.
     */
    public PigpioPulse(int gpioOn, int gpioOff, int delayMicros){
        this.gpioOn = 1 << gpioOn;
        this.gpioOff = 1 << gpioOff;
        this.delayMicros = delayMicros;
    }
}
