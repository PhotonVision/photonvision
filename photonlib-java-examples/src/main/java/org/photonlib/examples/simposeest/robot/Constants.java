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
package org.photonlib.examples.simposeest.robot;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
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
    public static final Transform2d kCameraToRobot =
            new Transform2d(
                    new Translation2d(-0.25, 0), // in meters
                    new Rotation2d());

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
    public static final double targetHeightAboveGround = Units.inchesToMeters(81.19); // meters

    // See https://firstfrc.blob.core.windows.net/frc2020/PlayingField/LayoutandMarkingDiagram.pdf
    // pages 4 and 5
    public static final double kFarTgtXPos = Units.feetToMeters(54);
    public static final double kFarTgtYPos =
            Units.feetToMeters(27 / 2) - Units.inchesToMeters(43.75) - Units.inchesToMeters(48.0 / 2.0);
    public static final Pose2d kFarTargetPose =
            new Pose2d(new Translation2d(kFarTgtXPos, kFarTgtYPos), new Rotation2d(0.0));

    public static final SimVisionTarget kFarTarget =
            new SimVisionTarget(kFarTargetPose, targetHeightAboveGround, targetWidth, targetHeight);
}
