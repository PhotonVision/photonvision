#!/usr/bin/env python3
###################################################################################
# MIT License
#
# Copyright (c) PhotonVision
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
###################################################################################

import math

import drivetrain
import wpilib
from photonlibpy import PhotonCamera

VISION_TURN_kP = 0.01
VISION_DES_ANGLE_deg = 0.0
VISION_STRAFE_kP = 0.5
VISION_DES_RANGE_m = 1.25
CAM_MOUNT_HEIGHT_m = 0.5  # Measured with a tape measure, or in CAD
CAM_MOUNT_PITCH_deg = -30.0  # Measured with a protractor, or in CAD
TAG_7_MOUNT_HEIGHT_m = 1.435  # From the 2024 game manual


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
        xSpeed = -1.0 * self.controller.getLeftY() * drivetrain.kMaxSpeed
        ySpeed = -1.0 * self.controller.getLeftX() * drivetrain.kMaxSpeed
        rot = -1.0 * self.controller.getRightX() * drivetrain.kMaxAngularSpeed

        # Get information from the camera
        targetYaw = 0.0
        targetRange = 0.0
        targetVisible = False
        results = self.cam.getAllUnreadResults()
        if len(results) > 0:
            result = results[-1]  # take the most recent result the camera had
            # At least one apriltag was seen by the camera
            for target in result.getTargets():
                if target.getFiducialId() == 7:
                    # Found tag 7, record its information
                    targetVisible = True
                    targetYaw = target.getYaw()
                    heightDelta = CAM_MOUNT_HEIGHT_m - TAG_7_MOUNT_HEIGHT_m
                    angleDelta = math.radians(CAM_MOUNT_PITCH_deg - target.getPitch())
                    targetRange = heightDelta / math.tan(angleDelta)

        if self.controller.getAButton() and targetVisible:
            # Driver wants auto-alignment to tag 7
            # And, tag 7 is in sight, so we can turn toward it.
            # Override the driver's turn and x-vel command with
            # an automatic one that turns toward the tag
            # and puts us at the right distance
            rot = (
                (VISION_DES_ANGLE_deg - targetYaw)
                * VISION_TURN_kP
                * drivetrain.kMaxAngularSpeed
            )
            xSpeed = (
                (VISION_DES_RANGE_m - targetRange)
                * VISION_STRAFE_kP
                * drivetrain.kMaxSpeed
            )

        self.swerve.drive(xSpeed, ySpeed, rot, True, self.getPeriod())

    def _simulationPeriodic(self) -> None:
        self.swerve.simulationPeriodic()
        return super()._simulationPeriodic()
