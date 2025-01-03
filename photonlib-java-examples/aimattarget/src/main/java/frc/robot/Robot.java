/*
 * MIT License
 *
 * Copyright (c) PhotonVision
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package frc.robot;

import org.photonvision.PhotonCamera;

import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.DMA;
import edu.wpi.first.wpilibj.DMASample;
import edu.wpi.first.wpilibj.DigitalOutput;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class Robot extends TimedRobot {
    PhotonCamera camera;
    DigitalOutput light;

    DMA dma;
    DMASample sample;

    @Override
    public void robotInit() {
        camera = new PhotonCamera("Arducam_OV9782_USB_Camera");

        light = new DigitalOutput(0);
        light.set(false);

        dma = new DMA();
        sample = new DMASample();

        dma.addDigitalSource(light);
        dma.setExternalTrigger(light, true, false);
        dma.start(1024);
    }

    @Override
    public void robotPeriodic() {
        super.robotPeriodic();

        try {
            light.set(false);
            for (int i = 0; i < 20; i++) {
                Thread.sleep(20);
                camera.getAllUnreadResults();
            }

            var t1 = Timer.getFPGATimestamp();
            light.set(true);
            var t2 = Timer.getFPGATimestamp();
            
            DMASample.DMAReadStatus readStatus = sample.update(dma, Units.millisecondsToSeconds(1));
            if (readStatus == DMASample.DMAReadStatus.kOk) { 
                var dmaTime = sample.getTimeStamp();
                SmartDashboard.putNumber("led_on_time_dma", dmaTime);
            }

            var t1p5 = (t1 + t2) / 2;
            SmartDashboard.putNumber("led_on_time_rio", t1p5);
            SmartDashboard.putNumber("led_set_dt", t2-t1);

            for (int i = 0; i < 100; i++) {
                for (var result : camera.getAllUnreadResults()) {
                    if (result.hasTargets()) {
                        var t3 = result.getTimestampSeconds();
                        SmartDashboard.putNumber("led_on_time_coproc", t3);
                        SmartDashboard.putNumber("led_time_photon_minus_rio", t3-t1p5);
                        return;
                    }
                }

                Thread.sleep(20);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
