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
import java.util.function.IntUnaryOperator;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import org.junit.jupiter.api.Test;
import org.photonvision.common.hardware.statusLED.LinearPattern;
import org.wpilib.math.util.Pair;

public class LinearPatternTest {
    LinearPattern pattern = new LinearPattern(11);

    static class PatternPanel extends JPanel implements ActionListener {
        final int pixelSize = 20;
        final int baseColor;
        final LinearPattern pattern;
        final IntUnaryOperator patternType;

        public PatternPanel(int baseColor, LinearPattern pattern, IntUnaryOperator patternType) {
            this.baseColor = baseColor;
            this.pattern = pattern;
            this.patternType = patternType;

            this.setPreferredSize(new Dimension(pixelSize * pattern.numPixels, pixelSize));

            new Timer(16, this).start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            for (pattern.pixel = 0; pattern.pixel < pattern.numPixels; pattern.pixel++) {
                g.setColor(new Color(patternType.applyAsInt(baseColor)));
                g.fillRect(pixelSize * (pattern.numPixels - pattern.pixel - 1), 0, pixelSize, pixelSize);
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

        final List<Pair<String, IntUnaryOperator>> allPatterns =
                List.of(
                        Pair.of("Blink", pattern::blink),
                        Pair.of("Throb", pattern::throb),
                        Pair.of("Phaser", pattern::phaser),
                        Pair.of("Converge", pattern::converge),
                        Pair.of("Diverge", pattern::diverge),
                        Pair.of("Slide Left", pattern::leftSlide),
                        Pair.of("Slide Right", pattern::rightSlide),
                        Pair.of("Hook Left", pattern::leftHook),
                        Pair.of("Hook Right", pattern::rightHook));

        var frame = new JFrame("Linear Pattern Demonstration");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));

        int i = 0;
        for (var patternPair : allPatterns) {
            JPanel panel =
                    new PatternPanel(
                            PixelColour.wheel(255 * i++ / allPatterns.size()), pattern, patternPair.getSecond());
            panel.add(new JLabel(patternPair.getFirst()));
            frame.add(panel);
        }

        frame.pack();
        frame.setVisible(true);

        while (frame.isVisible()) {}
    }
}
