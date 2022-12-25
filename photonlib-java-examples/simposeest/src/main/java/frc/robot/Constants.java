/*
 * MIT License
 *
 * Copyright (c) 2022 PhotonVision
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
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.kinematics.DifferentialDriveKinematics;
import edu.wpi.first.math.util.Units;
import org.photonvision.SimVisionTarget;

/**
 * Holding class for all physical constants that must be used throughout the codebase. These values
 * should be set by one of a few methods: 1) Talk to your mechanical and electrical teams and
 * determine how the physical robot is being built and configured. 2) Read the game manual and look
 * at the field drawings 3) Match with how your vision coprocessor is configured.
 */
public class Constants {
    //////////////////////////////////////////////////////////////////
    // Drivetrain Physical
    //////////////////////////////////////////////////////////////////
    public static final double kMaxSpeed = 3.0; // 3 meters per second.
    public static final double kMaxAngularSpeed = Math.PI; // 1/2 rotation per second.

    public static final double kTrackWidth = 0.381 * 2;
    public static final double kWheelRadius = 0.0508;
    public static final int kEncoderResolution = 4096;

    public static final DifferentialDriveKinematics kDtKinematics =
            new DifferentialDriveKinematics(kTrackWidth);

    //////////////////////////////////////////////////////////////////
    // Electrical IO
    //////////////////////////////////////////////////////////////////
    public static final int kGyroPin = 0;

    public static final int kDtLeftEncoderPinA = 0;
    public static final int kDtLeftEncoderPinB = 1;
    public static final int kDtRightEncoderPinA = 2;
    public static final int kDtRightEncoderPinB = 3;

    public static final int kDtLeftLeaderPin = 1;
    public static final int kDtLeftFollowerPin = 2;
    public static final int kDtRightLeaderPin = 3;
    public static final int kDtRightFollowerPin = 4;

    //////////////////////////////////////////////////////////////////
    // Vision Processing
    //////////////////////////////////////////////////////////////////
    // Name configured in the PhotonVision GUI for the camera
    public static final String kCamName = "mainCam";

    // Physical location of the camera on the robot, relative to the center of the
    // robot.
    public static final Transform3d kCameraToRobot =
            new Transform3d(
                    new Translation3d(-0.25, 0, -.25), // in meters
                    new Rotation3d());

    // See
    // https://firstfrc.blob.core.windows.net/frc2020/PlayingField/2020FieldDrawing-SeasonSpecific.pdf
    // page 208
    public static final double targetWidth =
            Units.inchesToMeters(41.30) - Units.inchesToMeters(6.70); // meters

    // See
    // https://firstfrc.blob.core.windows.net/frc2020/PlayingField/2020FieldDrawing-SeasonSpecific.pdf
    // page 197
    public static final double targetHeight =
            Units.inchesToMeters(98.19) - Units.inchesToMeters(81.19); // meters

    // See https://firstfrc.blob.core.windows.net/frc2020/PlayingField/LayoutandMarkingDiagram.pdf
    // pages 4 and 5
    public static final double kFarTgtXPos = Units.feetToMeters(54);
    public static final double kFarTgtYPos =
            Units.feetToMeters(27 / 2) - Units.inchesToMeters(43.75) - Units.inchesToMeters(48.0 / 2.0);
    public static final double kFarTgtZPos =
            (Units.inchesToMeters(98.19) - targetHeight) / 2 + targetHeight;

    public static final Pose3d kFarTargetPose =
            new Pose3d(
                    new Translation3d(kFarTgtXPos, kFarTgtYPos, kFarTgtZPos),
                    new Rotation3d(0.0, 0.0, Units.degreesToRadians(180)));

    public static final SimVisionTarget kFarTarget =
            new SimVisionTarget(kFarTargetPose, targetWidth, targetHeight, 42);
}
