package org.photonvision.common.hardware.GPIO;

public class CustomGPIO extends GPIOBase {

    private boolean currentState;
    private int port;

    public CustomGPIO(int port) {
        this.port = port;
    }

    @Override
    public void togglePin() {
        execute(
                commands
                        .get("setState")
                        .replace("{s}", String.valueOf(!currentState))
                        .replace("{p}", String.valueOf(this.port)));
        currentState = !currentState;
    }

    @Override
    public void setLow() {
        execute(
                commands
                        .get("setState")
                        .replace("{s}", String.valueOf(false))
                        .replace("{p}", String.valueOf(this.port)));
        currentState = false;
    }

    @Override
    public void setHigh() {
        execute(
                commands
                        .get("setState")
                        .replace("{s}", String.valueOf(true))
                        .replace("{p}", String.valueOf(this.port)));
        currentState = true;
    }

    @Override
    public void setState(boolean state) {
        execute(
                commands
                        .get("setState")
                        .replace("{s}", String.valueOf(state))
                        .replace("{p}", String.valueOf(this.port)));
        currentState = state;
    }

    @Override
    public void blink(long delay, long duration) {
        execute(
                commands
                        .get("setState")
                        .replace("{delay}", String.valueOf(delay))
                        .replace("{duration}", String.valueOf(duration))
                        .replace("{p}", String.valueOf(this.port)));
    }

    @Override
    public void pulse(long duration, boolean blocking) {
        execute(
                commands
                        .get("pulse")
                        .replace("{blocking}", String.valueOf(blocking))
                        .replace("{duration}", String.valueOf(duration))
                        .replace("{p}", String.valueOf(this.port)));
    }

    @Override
    public boolean shutdown() {
        execute(commands.get("shutdown"));
        return true;
    }

    @Override
    public boolean getState() {
        return currentState;
    }
}
