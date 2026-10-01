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
import wpimath
from photonlibpy import PhotonCamera, PhotonPoseEstimator
from robotpy_apriltag import AprilTagField, AprilTagFieldLayout

kRobotToCam = wpimath.Transform3d(
    wpimath.Translation3d(0.5, 0.0, 0.5),
    wpimath.Rotation3d.fromDegrees(0.0, -30.0, 0.0),
)

# Constants for filtering vision estimates.
kMaxAcceptedRobotZ = 0.2
kMaxAcceptedRobotPitch = 0.2
kMaxAcceptedRobotRoll = 0.2
kMaxAcceptedPoseAmbiguity = 0.2


class MyRobot(wpilib.TimedRobot):
    def __init__(self) -> None:
        """Robot initialization function"""
        super().__init__()

        self.controller = wpilib.NiDsXboxController(0)
        self.swerve = drivetrain.Drivetrain()
        self.cam = PhotonCamera("YOUR CAMERA NAME")
        self.aprilTagField = AprilTagFieldLayout.loadField(AprilTagField.kDefaultField)
        self.camPoseEst = PhotonPoseEstimator(
            self.aprilTagField,
            kRobotToCam,
        )

    def robotPeriodic(self) -> None:
        for result in self.cam.getAllUnreadResults():
            camEstPose = self.camPoseEst.estimateCoprocMultiTagPose(result)
            if camEstPose is None:
                camEstPose = self.camPoseEst.estimateLowestAmbiguityPose(result)

            # Filter out poses that are likely to be incorrect (off the field, not flat on the ground, etc).
            # This is optional, but can help prevent bad vision estimates from hurting your pose estimator.
            estPose = camEstPose.estimatedPose
            if (
                abs(estPose.Z()) < kMaxAcceptedRobotZ
                and -self.aprilTagField.getFieldLength() / 2
                < estPose.X()
                < self.aprilTagField.getFieldLength() / 2
                and -self.aprilTagField.getFieldWidth() / 2
                < estPose.Y()
                < self.aprilTagField.getFieldWidth() / 2
                and abs(estPose.rotation().X()) < kMaxAcceptedRobotPitch
                and abs(estPose.rotation().Y()) < kMaxAcceptedRobotRoll
                and result.getBestTarget().poseAmbiguity < kMaxAcceptedPoseAmbiguity
            ):
                self.swerve.addVisionPoseEstimate(
                    camEstPose.estimatedPose, camEstPose.timestampSeconds
                )

        self.swerve.updateOdometry()
        self.swerve.log()

    def teleopPeriodic(self) -> None:
        xSpeed = -1.0 * self.controller.getLeftY() * drivetrain.kMaxSpeed
        ySpeed = -1.0 * self.controller.getLeftX() * drivetrain.kMaxSpeed
        rot = -1.0 * self.controller.getRightX() * drivetrain.kMaxAngularSpeed

        self.swerve.drive(xSpeed, ySpeed, rot, True, self.getPeriod())

    def simulationPeriodic(self) -> None:
        self.swerve.simulationPeriodic()
        return super().simulationPeriodic()
