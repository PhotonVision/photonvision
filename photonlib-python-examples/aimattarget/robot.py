#!/usr/bin/env python3
#
# Copyright (c) FIRST and other WPILib contributors.
# Open Source Software; you can modify and/or share it under the terms of
# the WPILib BSD license file in the root directory of this project.
#

import wpilib
import drivetrain

from photonlibpy import PhotonCamera

VISION_TURN_kP = 0.01

class MyRobot(wpilib.TimedRobot):
    def robotInit(self) -> None:
        """Robot initialization function"""
        self.controller = wpilib.XboxController(0)
        self.swerve = drivetrain.Drivetrain()
        self.cam = PhotonCamera("YOUR CAMERA NAME")

    def robotPeriodic(self) -> None:
        self.swerve.updateOdometry()
        self.swerve.log()

    def teleopPeriodic(self) -> None:
        xSpeed = -1.0 * self.controller.getLeftY()  * drivetrain.kMaxSpeed
        ySpeed = -1.0 * self.controller.getLeftX()  * drivetrain.kMaxSpeed
        rot    = -1.0 * self.controller.getRightX() * drivetrain.kMaxAngularSpeed

        # Get information from the camera
        targetYaw = 0.0
        targetVisible = False
        results = self.cam.getAllUnreadResults()
        if(len(results) > 0):
            result = results[-1] #take the most recent result the camera had
            if result.hasTargets():
                # At least one apriltag was seen by the camera
                for target in result.getTargets():
                    if target.getFiducialId() == 7:
                        # Found tag 7, record its information
                        targetVisible = True
                        targetYaw = target.getYaw()

        if(self.controller.getAButton() and targetVisible):
            # Driver wants auto-alignment to tag 7
            # And, tag 7 is in sight, so we can turn toward it.
            # Override the driver's turn command with an automatic one that turns toward the tag.
            rot = -1.0 * targetYaw * VISION_TURN_kP * drivetrain.kMaxAngularSpeed

        self.swerve.drive(xSpeed, ySpeed, rot, True, self.getPeriod())

    def _simulationPeriodic(self) -> None:
        self.swerve.simulationPeriodic()
        return super()._simulationPeriodic()