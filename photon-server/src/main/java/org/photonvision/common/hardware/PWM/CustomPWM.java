package org.photonvision.common.hardware.PWM;

public class CustomPWM extends PWMBase {
    private int pwmRate = 0;
    private int pwmRange = 0;
    private int port = 0;

    public CustomPWM(int port) {
        this.port = port;
    }

    @Override
    public void setPwmRate(int rate) {
        execute(
                commands
                        .get("setRate")
                        .replace("{rate}", String.valueOf(rate))
                        .replace("{p}", String.valueOf(port)));
        pwmRate = rate;
    }

    @Override
    public void setPwmRange(int range) {
        execute(
                commands
                        .get("setRange")
                        .replace("{range}", String.valueOf(range))
                        .replace("{p}", String.valueOf(port)));
        pwmRange = range;
    }

    @Override
    public int getPwmRate() {
        return pwmRate;
    }

    @Override
    public int getPwmRange() {
        return pwmRange;
    }

    @Override
    public boolean shutdown() {
        execute(commands.get("shutdown"));
        return true;
    }
}
