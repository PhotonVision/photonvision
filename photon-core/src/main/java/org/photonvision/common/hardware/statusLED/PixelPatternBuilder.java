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

package org.photonvision.common.hardware.statusLED;

import com.diozero.ws281xj.PixelColour;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleFunction;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class PixelPatternBuilder {
    public final int numPixels;

    public static record PixelData(int position, int color) {}

    /** Pattern that treats all pixels identically */
    public abstract class PixelPattern implements Iterable<PixelData> {
        protected int numPixels = PixelPatternBuilder.this.numPixels;

        protected PixelPattern() {}

        /** Fades an individual component by the specified percentage */
        protected static int fadeComponent(int baseComponent, double percentage) {
            // return (int) (baseComponent * percentage);
            // Perceptually uniform fade; unnecessarily complex
            return (int) (255.0 * Math.pow(Math.pow(baseComponent / 255.0, 2.2) * percentage, 1.0 / 2.2));
        }

        /** Fades the individual components of a color by the specified percentage */
        protected static int fade(int baseColor, double percentage) {
            if (percentage == 1.0) {
                return baseColor;
            } else if (percentage == 0.0) {
                return 0;
            } else {
                return PixelColour.createColourRGB(
                        fadeComponent(PixelColour.getRedComponent(baseColor), percentage),
                        fadeComponent(PixelColour.getGreenComponent(baseColor), percentage),
                        fadeComponent(PixelColour.getBlueComponent(baseColor), percentage));
            }
        }

        /** Time between 0.0 and 1.0 within the LED animation loop */
        protected static double loopTime() {
            return System.currentTimeMillis() / 2000.0 % 1.0;
        }

        public Iterable<PixelData> wrappedBy(SinglePixelPattern outer) {
            numPixels -= 2;
            return () ->
                    Stream.of(
                                    Stream.of(new PixelData(0, outer.color())),
                                    stream().map(pixel -> new PixelData(pixel.position + 1, pixel.color)),
                                    Stream.of(new PixelData(numPixels + 1, outer.color())))
                            .flatMap(Function.identity())
                            .iterator();
        }

        public class PatternIterator implements Iterator<PixelData> {
            protected int pixel = 0;

            final DoubleToIntFunction colorSampler;

            PatternIterator(DoubleToIntFunction colorSampler) {
                this.colorSampler = colorSampler;
            }

            /** Position of the current pixel, normalized from zero to one */
            public double position() {
                return ((double) pixel + 0.5) / (double) numPixels;
            }

            @Override
            public boolean hasNext() {
                return pixel < numPixels;
            }

            @Override
            public PixelData next() {
                var data = new PixelData(pixel, colorSampler.applyAsInt(position()));
                pixel++;
                return data;
            }
        }

        @Override
        public Spliterator<PixelData> spliterator() {
            return Spliterators.spliterator(
                    iterator(), numPixels, Spliterator.NONNULL | Spliterator.ORDERED);
        }

        public Stream<PixelData> stream() {
            return StreamSupport.stream(spliterator(), false);
        }
    }

    // public Iterable<PixelData> wrappedBy(FlatPixelPatternType type, int baseColor) {
    //     var outer = new PixelPattern(1, type, baseColor);
    //     var inner = new PixelPattern(numPixels - 2, this.type, this.baseColor);
    //     return () -> {
    //         return Stream.of(outer.stream(), inner.stream(), outer.stream())
    //                 .flatMap(Function.identity())
    //                 .iterator();
    //     };
    // }

    public class SinglePixelPattern extends PixelPattern {
        final IntSupplier colorSampler;

        protected SinglePixelPattern(IntSupplier colorSampler) {
            this.colorSampler = colorSampler;
        }

        protected SinglePixelPattern(DoubleToIntFunction colorSampler) {
            this(() -> colorSampler.applyAsInt(loopTime()));
        }

        protected SinglePixelPattern(int baseColor, DoubleUnaryOperator brightnessSampler) {
            this((double time) -> fade(baseColor, brightnessSampler.applyAsDouble(time)));
        }

        public int color() {
            return colorSampler.getAsInt();
        }

        @Override
        public Iterator<PixelData> iterator() {
            return new PatternIterator((double position) -> colorSampler.getAsInt());
        }
    }

    protected class LinearPixelPattern extends PixelPattern {
        final Supplier<DoubleToIntFunction> colorSamplerProvider;

        LinearPixelPattern(int baseColor, Supplier<DoubleUnaryOperator> brightnessSamplerProvider) {
            this.colorSamplerProvider =
                    () -> {
                        var brightnessSampler = brightnessSamplerProvider.get();
                        return (double position) -> fade(baseColor, brightnessSampler.applyAsDouble(position));
                    };
        }

        LinearPixelPattern(
                int baseColor, DoubleFunction<DoubleUnaryOperator> brightnessSamplerTimeFunction) {
            this(baseColor, () -> brightnessSamplerTimeFunction.apply(loopTime()));
        }

        LinearPixelPattern(int baseColor, DoubleBinaryOperator brightnessSampler) {
            this(
                    baseColor,
                    (double loopTime) ->
                            (double position) -> brightnessSampler.applyAsDouble(loopTime, position));
        }

        @Override
        public Iterator<PixelData> iterator() {
            return new PatternIterator(colorSamplerProvider.get());
        }
    }

    protected class RevolvingLinearPixelPattern extends LinearPixelPattern {
        RevolvingLinearPixelPattern(
                int baseColor, DoubleFunction<DoubleBinaryOperator> brightnessSamplerTimeFunction) {
            super(
                    baseColor,
                    (double loopTime) -> {
                        var brightnessSampler = brightnessSamplerTimeFunction.apply(loopTime);
                        var phase = 2.0 * Math.PI * loopTime;
                        var center = (Math.sin(phase) + 1) / 2;
                        var trail = 1 / (0.75 * Math.cos(phase) + Math.copySign(0.25, Math.cos(phase)));
                        return (double position) ->
                                brightnessSampler.applyAsDouble(position, (center - position) * trail);
                    });
        }
    }

    public PixelPatternBuilder(int numPixels) {
        this.numPixels = numPixels;
    }

    /**
     * Calculate fade percentage based on distance from light center; quickly on the positive side,
     * slowly on the negative side
     */
    protected static double fadeout(double distance) {
        return Math.exp(-(distance >= 0 ? 20 : 200) * Math.pow(distance, 2.0));
    }

    /**
     * Calculate fade percentage based on cyclic distance from light center; helps blend discontinuous
     * animations
     */
    protected static double cyclicFadeout(double distance) {
        return fadeout(((distance + 1.5) % 1.0) - 0.5);
    }

    // All patterns can be implemented as a function of time yielding a function of position (or
    // constant)
    // Arbitrary things can be cached by the time function as inputs to the position function

    public SinglePixelPattern solid(int color) {
        return new SinglePixelPattern(() -> color);
    }

    public SinglePixelPattern blink(int baseColor) {
        return new SinglePixelPattern((double loopTime) -> loopTime > 0.4 ? baseColor : 0);
    }

    public SinglePixelPattern doubleBlink(int baseColor) {
        return new SinglePixelPattern(
                (double loopTime) -> Math.abs(Math.abs(loopTime - 0.5) - 0.15) < 0.05 ? baseColor : 0);
    }

    public SinglePixelPattern throb(int baseColor) {
        return new SinglePixelPattern(
                baseColor, (double loopTime) -> (Math.sin(2.0 * Math.PI * loopTime) + 1) / 2);
    }

    public PixelPattern phaser(int baseColor) {
        return new RevolvingLinearPixelPattern(
                baseColor, (double loopTime) -> (double position, double revolve) -> fadeout(revolve));
    }

    public PixelPattern converge(int baseColor) {
        return new LinearPixelPattern(
                baseColor,
                (double loopTime, double position) ->
                        cyclicFadeout(loopTime + Math.abs(position - 0.5) * 2.0 - 1.0));
    }

    public PixelPattern diverge(int baseColor) {
        return new LinearPixelPattern(
                baseColor,
                (double loopTime, double position) ->
                        cyclicFadeout(loopTime - Math.abs(position - 0.5) * 2.0));
    }

    public PixelPattern slideLeft(int baseColor) {
        return new LinearPixelPattern(
                baseColor, (double loopTime, double position) -> cyclicFadeout(loopTime - position));
    }

    public PixelPattern slideRight(int baseColor) {
        return new LinearPixelPattern(
                baseColor, (double loopTime, double position) -> cyclicFadeout(loopTime + position - 1.0));
    }

    public PixelPattern rotateLeft(int baseColor) {
        return new RevolvingLinearPixelPattern(
                baseColor,
                (double loopTime) -> {
                    final double depthFade = (Math.cos(2.0 * Math.PI * loopTime) + 1.1) / 2.1;
                    return (double position, double revolve) -> fadeout(revolve) * depthFade;
                });
    }

    public PixelPattern rotateRight(int baseColor) {
        return new RevolvingLinearPixelPattern(
                baseColor,
                (double loopTime) -> {
                    final double depthFade = (-Math.cos(2.0 * Math.PI * loopTime) + 1.1) / 2.1;
                    return (double position, double revolve) -> fadeout(revolve) * depthFade;
                });
    }
}
