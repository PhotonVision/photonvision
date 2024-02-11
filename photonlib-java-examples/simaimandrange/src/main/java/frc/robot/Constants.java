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

import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.math.util.Units;

public class Constants {
    // ---------- Vision
    // Constants about how your camera is mounted to the robot
    public static final double CAMERA_PITCH_RADIANS =
            Units.degreesToRadians(15); // Angle "up" from horizontal
    public static final double CAMERA_HEIGHT_METERS = Units.inchesToMeters(24); // Height above floor

    // How far from the target we want to be
    public static final double GOAL_RANGE_METERS = Units.feetToMeters(10);

    // Where the 2020 High goal target is located on the field
    // See
    // https://docs.wpilib.org/en/stable/docs/software/advanced-controls/geometry/coordinate-systems.html#field-coordinate-system
    // and https://firstfrc.blob.core.windows.net/frc2020/PlayingField/LayoutandMarkingDiagram.pdf
    // (pages 4 and 5)
    public static final Pose3d TARGET_POSE =
            new Pose3d(
                    new Translation3d(
                            Units.feetToMeters(52.46),
                            Units.inchesToMeters(94.66),
                            Units.inchesToMeters(89.69)), // (center of vision target)
                    new Rotation3d(0.0, 0.0, Math.PI));
    // ----------

    // ---------- Drivetrain
    public static final int LEFT_MOTOR_CHANNEL = 0;
    public static final int RIGHT_MOTOR_CHANNEL = 1;

    // PID constants should be tuned per robot
    public static final double LINEAR_P = 0.5;
    public static final double LINEAR_I = 0;
    public static final double LINEAR_D = 0.1;

    public static final double ANGULAR_P = 0.03;
    public static final double ANGULAR_I = 0;
    public static final double ANGULAR_D = 0.003;

    // Ratio to multiply joystick inputs by
    public static final double DRIVESPEED = 0.75;

    // The following properties are necessary for simulation:

    // Distance from drivetrain left wheels to right wheels
    public static final double TRACKWIDTH_METERS = Units.feetToMeters(2.0);
    public static final double WHEEL_DIAMETER_METERS = Units.inchesToMeters(6.0);

    // The motors used in the gearbox for one drivetrain side
    public static final DCMotor DRIVE_MOTORS = DCMotor.getCIM(2);
    // The gearbox reduction, or how many motor rotations per wheel rotation
    public static final double GEARING = 8.0;

    // The drivetrain feedforward values
    public static final double LINEAR_KV = 2.0;
    public static final double LINEAR_KA = 0.5;

    public static final double ANGULAR_KV = 2.25;
    public static final double ANGULAR_KA = 0.3;
    // ----------
}
