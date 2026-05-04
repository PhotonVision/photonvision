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


import drivetrain
import wpilib
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
        xSpeed = -1.0 * self.controller.getLeftY() * drivetrain.kMaxSpeed
        ySpeed = -1.0 * self.controller.getLeftX() * drivetrain.kMaxSpeed
        rot = -1.0 * self.controller.getRightX() * drivetrain.kMaxAngularSpeed

        # Get information from the camera
        targetYaw = 0.0
        targetVisible = False
        results = self.cam.getAllUnreadResults()
        if len(results) > 0:
            result = results[-1]  # take the most recent result the camera had
            for target in result.getTargets():
                if target.getFiducialId() == 7:
                    # Found tag 7, record its information
                    targetVisible = True
                    targetYaw = target.getYaw()

        if self.controller.getAButton() and targetVisible:
            # Driver wants auto-alignment to tag 7
            # And, tag 7 is in sight, so we can turn toward it.
            # Override the driver's turn command with an automatic one that turns toward the tag.
            rot = -1.0 * targetYaw * VISION_TURN_kP * drivetrain.kMaxAngularSpeed

        self.swerve.drive(xSpeed, ySpeed, rot, True, self.getPeriod())

    def _simulationPeriodic(self) -> None:
        self.swerve.simulationPeriodic()
        return super()._simulationPeriodic()
