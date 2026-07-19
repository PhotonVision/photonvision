package org.photonvision.common.hardware.statusLED;

import com.diozero.ws281xj.PixelColour;

public class LinearPattern {
    public final int numPixels;
    public int pixel = 0;

    public LinearPattern(int numPixels) {
        this.numPixels = numPixels;
    }

    protected double loopTime() {
        return System.currentTimeMillis() / 2000.0 % 1.0;
    }

    /** Position of the current pixel, normalized from zero to one */
    protected double position() {
        return ((double) pixel + 0.5) / (double) numPixels;
    }

    /**
     * Calculate fade percentage based on distance from light center; quickly on the positive side,
     * slowly on the negative side
     */
    protected double fadeout(double distance) {
        return Math.exp(-(distance >= 0 ? 20 : 200) * Math.pow(distance, 2.0));
    }

    /**
     * Calculate fade percentage based on cyclic distance from light center; helps blend discontinuous
     * animations
     */
    protected double cyclicFadeout(double distance) {
        return fadeout(((distance + 1.5) % 1.0) - 0.5);
    }

    protected double revolve(double time) {
        var phase = 2.0 * Math.PI * time;
        return ((Math.sin(phase) + 1) / 2 - position())
                / (0.75 * Math.cos(phase) + Math.copySign(0.25, Math.cos(phase)));
    }

    /** Fades an individual component by the specified percentage */
    protected int fadeComponent(int baseComponent, double percentage) {
        return (int) (baseComponent * percentage);
        // Perceptually uniform fade; unnecessarily complex
        // return (int) (255.0 * Math.pow(Math.pow(baseComponent / 255.0, 2.2) * percentage, 1.0 /
        // 2.2));
    }

    /** Fades the individual components of a color by the specified percentage */
    protected int fade(int baseColor, double percentage) {
        return PixelColour.createColourRGB(
                fadeComponent(PixelColour.getRedComponent(baseColor), percentage),
                fadeComponent(PixelColour.getGreenComponent(baseColor), percentage),
                fadeComponent(PixelColour.getBlueComponent(baseColor), percentage));
    }

    /** Blink the whole bar */
    public int blink(int baseColor) {
        return loopTime() > 0.4 ? baseColor : 0;
    }

    /** Fade in and out over the whole bar */
    public int throb(int baseColor) {
        return fade(baseColor, (Math.sin(2.0 * Math.PI * loopTime()) + 1) / 2);
    }

    /** Oscillate left and right across the bar */
    public int phaser(int baseColor) {
        return fade(baseColor, fadeout(revolve(loopTime())));
    }

    /** Converge towards the center of the bar */
    public int converge(int baseColor) {
        return fade(baseColor, cyclicFadeout(loopTime() + Math.abs(position() - 0.5) * 2.0 - 1.0));
    }

    /** Diverge away from the center of the bar */
    public int diverge(int baseColor) {
        return fade(baseColor, cyclicFadeout(loopTime() - Math.abs(position() - 0.5) * 2.0));
    }

    /** Slide left across the bar */
    public int leftSlide(int baseColor) {
        return fade(baseColor, cyclicFadeout(loopTime() - position()));
    }

    /** Slide right across the bar */
    public int rightSlide(int baseColor) {
        return fade(baseColor, cyclicFadeout(loopTime() + position() - 1.0));
    }

    /** Slide left with a hook right near the end of the bar */
    public int leftHook(int baseColor) {
        return fade(
                baseColor,
                fadeout(revolve(loopTime())) * (Math.cos(2.0 * Math.PI * loopTime()) + 1.1) / 2.1);
    }

    /** Slide right with a hook left near the end of the bar */
    public int rightHook(int baseColor) {
        return fade(
                baseColor,
                fadeout(revolve(loopTime())) * (-Math.cos(2.0 * Math.PI * loopTime()) + 1.1) / 2.1);
    }
}
