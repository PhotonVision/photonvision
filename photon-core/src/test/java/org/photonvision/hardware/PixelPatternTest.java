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

package org.photonvision.hardware;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

import com.diozero.ws281xj.PixelColour;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.function.IntFunction;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import org.junit.jupiter.api.Test;
import org.photonvision.common.hardware.statusLED.PixelPatternBuilder;
import org.photonvision.common.hardware.statusLED.PixelPatternBuilder.PixelData;
import org.wpilib.math.util.Pair;

public class PixelPatternTest {
    PixelPatternBuilder patternBuilder = new PixelPatternBuilder(11);

    class PatternPanel extends JPanel implements ActionListener {
        final int pixelSize = 20;
        final Iterable<PixelData> pattern;

        public PatternPanel(Iterable<PixelData> pattern) {
            this.pattern = pattern;

            this.setPreferredSize(new Dimension(pixelSize * patternBuilder.numPixels, pixelSize));

            new Timer(16, this).start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            for (var pixel : pattern) {
                if (pixel.position() >= patternBuilder.numPixels) {
                    throw new ArrayIndexOutOfBoundsException(pixel.position());
                }
                g.setColor(new Color(pixel.color()));
                g.fillRect(
                        pixelSize * (patternBuilder.numPixels - 1 - pixel.position()), 0, pixelSize, pixelSize);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            this.repaint();
        }
    }

    @Test

    /** Create a basic UI showing the patterns, backed by the Java impl of LinearPattern. */
    void patternDemonstration() {
        assumeFalse(GraphicsEnvironment.isHeadless());

        final List<Pair<String, IntFunction<Iterable<PixelData>>>> allPatterns =
                List.of(
                        Pair.of("Blink", patternBuilder::blink),
                        Pair.of("Double Blink", patternBuilder::doubleBlink),
                        Pair.of("Throb", patternBuilder::throb),
                        Pair.of("Phaser", patternBuilder::phaser),
                        Pair.of("Converge", patternBuilder::converge),
                        Pair.of("Diverge", patternBuilder::diverge),
                        Pair.of("Slide Left", patternBuilder::slideLeft),
                        Pair.of("Slide Right", patternBuilder::slideRight),
                        Pair.of("Rotate Left", patternBuilder::rotateLeft),
                        Pair.of("Rotate Right", patternBuilder::rotateRight),
                        Pair.of(
                                "Wrapped Phaser",
                                (int innerColor) ->
                                        patternBuilder
                                                .phaser(innerColor)
                                                .wrappedBy(patternBuilder.doubleBlink(0xFFFFFF))));

        var frame = new JFrame("Linear Pattern Demonstration");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));

        int i = 0;
        for (var patternPair : allPatterns) {
            JPanel panel =
                    new PatternPanel(
                            patternPair.getSecond().apply(PixelColour.wheel(255 * i++ / allPatterns.size())));
            panel.add(new JLabel(patternPair.getFirst()));
            frame.add(panel);
        }

        frame.pack();
        frame.setVisible(true);

        while (frame.isVisible()) {}
    }
}
