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

package frc.robot.subsystems;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.math.system.plant.LinearSystemId;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.motorcontrol.PWMSparkMax;
import edu.wpi.first.wpilibj.simulation.FlywheelSim;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class GamepieceLauncher {
    private final PWMSparkMax motor;

    private final double LAUNCH_SPEED_RPM = 2500;
    private double curDesSpd;
    private double curMotorCmd = 0.0;

    public GamepieceLauncher() {
        motor = new PWMSparkMax(8);
        simulationInit();
    }

    public void setRunning(boolean shouldRun) {
        curDesSpd = shouldRun ? LAUNCH_SPEED_RPM : 0.0;
    }

    public void periodic() {
        double maxRPM =
                Units.radiansPerSecondToRotationsPerMinute(DCMotor.getFalcon500(1).freeSpeedRadPerSec);
        curMotorCmd = curDesSpd / maxRPM;
        curMotorCmd = MathUtil.clamp(curMotorCmd, 0.0, 1.0);
        motor.set(curMotorCmd);

        SmartDashboard.putNumber("GPLauncher Des Spd (RPM)", curDesSpd);
    }

    // -- SIMULATION SUPPORT
    private DCMotor motorSim;
    private FlywheelSim launcherSim;
    private final double flywheelMoiKgM2 = 0.002;
    private final double flywheelGearRatio = 1.0;

    private void simulationInit() {
        motorSim = DCMotor.getFalcon500(1);
        launcherSim =
                new FlywheelSim(
                        LinearSystemId.createFlywheelSystem(motorSim, flywheelMoiKgM2, flywheelGearRatio),
                        motorSim);
    }

    public void simulationPeriodic() {
        launcherSim.setInputVoltage(curMotorCmd * RobotController.getBatteryVoltage());
        launcherSim.update(0.02);
        var spd = launcherSim.getAngularVelocityRPM();
        SmartDashboard.putNumber("GPLauncher Act Spd (RPM)", spd);
    }
}
