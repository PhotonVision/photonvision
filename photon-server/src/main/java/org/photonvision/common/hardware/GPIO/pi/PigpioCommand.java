package org.photonvision.common.hardware.GPIO.pi;

@SuppressWarnings("SpellCheckingInspection")
public enum PigpioCommand {
    PCMD_READ(3), // int gpio_read(unsigned gpio)
    PCMD_WRITE(4), // int gpio_write(unsigned gpio, unsigned level)
    PCMD_WVCLR(27), // int wave_clear(void)
    PCMD_WVAG(28), // int wave_add_generic(unsigned numPulses, gpioPulse_t *pulses)
    PCMD_WVHLT(33), // int wave_tx_stop(void)
    PCMD_WVCRE(49), // int wave_create(void)
    PCMD_WVDEL(50), // int wave_delete(unsigned wave_id)
    PCMD_WVTX(51), // int wave_tx_send(unsigned wave_id) (once)
    PCMD_WVTXR(52), // int wave_tx_send(unsigned wave_id) (repeat)
    PCMD_GDC(83), // int get_duty_cyle(unsigned user_gpio)
    PCMD_HP(86), // int hardware_pwm(unsigned gpio, unsigned PWMfreq, unsigned PWMduty)
    PCMD_WVTXM(100); // int wave_tx_send(unsigned wave_id, unsigned wave_mode)

    public final int value;

    PigpioCommand(int value) {
        this.value = value;
    }
}
