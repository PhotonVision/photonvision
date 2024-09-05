package frc.robot.subsystems;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.motorcontrol.PWMSparkMax;
import edu.wpi.first.wpilibj.simulation.EncoderSim;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class GamepieceLauncher {

    private final PWMSparkMax motor;
    private final Encoder encoder;

    private final double LAUNCH_SPEED_RPM = 2500;
    private double curDesSpd;
    private double curMotorCmd = 0.0;

    public GamepieceLauncher() {
        motor = new PWMSparkMax(8);
        encoder = new Encoder(16, 17);
        simulationInit();
    }

    public void setRunning(boolean shouldRun) {
        curDesSpd = shouldRun ? LAUNCH_SPEED_RPM : 0.0;
    }

    public void periodic() {
        double actSpd =encoder.getRate();
        double err = curDesSpd - actSpd;
        curMotorCmd = 1.0 * err;
        curMotorCmd = MathUtil.clamp(curMotorCmd, 0.0, 1.0);
        motor.set(curMotorCmd);

        SmartDashboard.putNumber("GPLauncher Des Spd (RPM)", curDesSpd);
        SmartDashboard.putNumber("GPLauncher Act Spd (RPM)", actSpd);

    }

    // -- SIMULATION SUPPORT
    private DCMotor motorSim;
    private EncoderSim encoderSim;

    private void simulationInit() {
        motorSim = DCMotor.getFalcon500(1);
        encoderSim = new EncoderSim(encoder);
    }

    public void simulationPeriodic() {
        var spd = motorSim.getSpeed(0.0, curMotorCmd*RobotController.getBatteryVoltage());
        encoderSim.setRate(Units.radiansPerSecondToRotationsPerMinute(spd));
    }

}
