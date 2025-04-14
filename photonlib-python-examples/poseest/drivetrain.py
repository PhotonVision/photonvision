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

import swervemodule
import wpilib
import wpilib.simulation
import wpimath.estimator
import wpimath.geometry
import wpimath.kinematics

kMaxSpeed = 3.0  # 3 meters per second
kMaxAngularSpeed = math.pi  # 1/2 rotation per second

kInitialPose = wpimath.geometry.Pose2d(
    wpimath.geometry.Translation2d(1.0, 1.0),
    wpimath.geometry.Rotation2d.fromDegrees(0.0),
)


class Drivetrain:
    """
    Represents a swerve drive style drivetrain.
    """

    def __init__(self) -> None:
        self.frontLeftLocation = wpimath.geometry.Translation2d(0.381, 0.381)
        self.frontRightLocation = wpimath.geometry.Translation2d(0.381, -0.381)
        self.backLeftLocation = wpimath.geometry.Translation2d(-0.381, 0.381)
        self.backRightLocation = wpimath.geometry.Translation2d(-0.381, -0.381)

        self.frontLeft = swervemodule.SwerveModule(1, 2, 0, 1, 2, 3, 1)
        self.frontRight = swervemodule.SwerveModule(3, 4, 4, 5, 6, 7, 2)
        self.backLeft = swervemodule.SwerveModule(5, 6, 8, 9, 10, 11, 3)
        self.backRight = swervemodule.SwerveModule(7, 8, 12, 13, 14, 15, 4)

        self.debugField = wpilib.Field2d()
        wpilib.SmartDashboard.putData("Drivetrain Debug", self.debugField)

        self.gyro = wpilib.AnalogGyro(0)
        self.simGyro = wpilib.simulation.AnalogGyroSim(self.gyro)

        self.kinematics = wpimath.kinematics.SwerveDrive4Kinematics(
            self.frontLeftLocation,
            self.frontRightLocation,
            self.backLeftLocation,
            self.backRightLocation,
        )

        self.poseEst = wpimath.estimator.SwerveDrive4PoseEstimator(
            self.kinematics,
            self.gyro.getRotation2d(),
            (
                self.frontLeft.getPosition(),
                self.frontRight.getPosition(),
                self.backLeft.getPosition(),
                self.backRight.getPosition(),
            ),
            kInitialPose,
        )

        self.targetChassisSpeeds = wpimath.kinematics.ChassisSpeeds()

        self.gyro.reset()

    def drive(
        self,
        xSpeed: float,
        ySpeed: float,
        rot: float,
        fieldRelative: bool,
        periodSeconds: float,
    ) -> None:
        """
        Method to drive the robot using joystick info.
        :param xSpeed: Speed of the robot in the x direction (forward).
        :param ySpeed: Speed of the robot in the y direction (sideways).
        :param rot: Angular rate of the robot.
        :param fieldRelative: Whether the provided x and y speeds are relative to the field.
        :param periodSeconds: Time
        """
        swerveModuleStates = self.kinematics.toSwerveModuleStates(
            wpimath.kinematics.ChassisSpeeds.discretize(
                (
                    wpimath.kinematics.ChassisSpeeds.fromFieldRelativeSpeeds(
                        xSpeed, ySpeed, rot, self.gyro.getRotation2d()
                    )
                    if fieldRelative
                    else wpimath.kinematics.ChassisSpeeds(xSpeed, ySpeed, rot)
                ),
                periodSeconds,
            )
        )
        wpimath.kinematics.SwerveDrive4Kinematics.desaturateWheelSpeeds(
            swerveModuleStates, kMaxSpeed
        )
        self.frontLeft.setDesiredState(swerveModuleStates[0])
        self.frontRight.setDesiredState(swerveModuleStates[1])
        self.backLeft.setDesiredState(swerveModuleStates[2])
        self.backRight.setDesiredState(swerveModuleStates[3])

        self.targetChassisSpeeds = self.kinematics.toChassisSpeeds(swerveModuleStates)

    def updateOdometry(self) -> None:
        """Updates the field relative position of the robot."""
        self.poseEst.update(
            self.gyro.getRotation2d(),
            (
                self.frontLeft.getPosition(),
                self.frontRight.getPosition(),
                self.backLeft.getPosition(),
                self.backRight.getPosition(),
            ),
        )

    def addVisionPoseEstimate(
        self, pose: wpimath.geometry.Pose3d, timestamp: float
    ) -> None:
        self.poseEst.addVisionMeasurement(pose, timestamp)

    def resetPose(self) -> None:
        self.poseEst.resetPosition(
            self.gyro.getRotation2d(),
            (
                self.frontLeft.getPosition(),
                self.frontRight.getPosition(),
                self.backLeft.getPosition(),
                self.backRight.getPosition(),
            ),
            kInitialPose,
        )

    def getModuleStates(self) -> list[wpimath.kinematics.SwerveModuleState]:
        return [
            self.frontLeft.getState(),
            self.frontRight.getState(),
            self.backLeft.getState(),
            self.backRight.getState(),
        ]

    def getModulePoses(self) -> list[wpimath.geometry.Pose2d]:
        p = self.poseEst.getEstimatedPosition()
        flTrans = wpimath.geometry.Transform2d(
            self.frontLeftLocation, self.frontLeft.getAbsoluteHeading()
        )
        frTrans = wpimath.geometry.Transform2d(
            self.frontRightLocation, self.frontRight.getAbsoluteHeading()
        )
        blTrans = wpimath.geometry.Transform2d(
            self.backLeftLocation, self.backLeft.getAbsoluteHeading()
        )
        brTrans = wpimath.geometry.Transform2d(
            self.backRightLocation, self.backRight.getAbsoluteHeading()
        )
        return [
            p.transformBy(flTrans),
            p.transformBy(frTrans),
            p.transformBy(blTrans),
            p.transformBy(brTrans),
        ]

    def getChassisSpeeds(self) -> wpimath.kinematics.ChassisSpeeds:
        return self.kinematics.toChassisSpeeds(self.getModuleStates())

    def log(self):
        table = "Drive/"

        pose = self.poseEst.getEstimatedPosition()
        wpilib.SmartDashboard.putNumber(table + "X", pose.X())
        wpilib.SmartDashboard.putNumber(table + "Y", pose.Y())
        wpilib.SmartDashboard.putNumber(table + "Heading", pose.rotation().degrees())

        chassisSpeeds = self.getChassisSpeeds()
        wpilib.SmartDashboard.putNumber(table + "VX", chassisSpeeds.vx)
        wpilib.SmartDashboard.putNumber(table + "VY", chassisSpeeds.vy)
        wpilib.SmartDashboard.putNumber(
            table + "Omega Degrees", chassisSpeeds.omega_dps
        )

        wpilib.SmartDashboard.putNumber(
            table + "Target VX", self.targetChassisSpeeds.vx
        )
        wpilib.SmartDashboard.putNumber(
            table + "Target VY", self.targetChassisSpeeds.vy
        )
        wpilib.SmartDashboard.putNumber(
            table + "Target Omega Degrees", self.targetChassisSpeeds.omega_dps
        )

        self.frontLeft.log()
        self.frontRight.log()
        self.backLeft.log()
        self.backRight.log()

        self.debugField.getRobotObject().setPose(self.poseEst.getEstimatedPosition())
        self.debugField.getObject("SwerveModules").setPoses(self.getModulePoses())

    def simulationPeriodic(self):
        self.frontLeft.simulationPeriodic()
        self.frontRight.simulationPeriodic()
        self.backLeft.simulationPeriodic()
        self.backRight.simulationPeriodic()
        self.simGyro.setRate(-1.0 * self.getChassisSpeeds().omega_dps)
        self.simGyro.setAngle(self.simGyro.getAngle() + self.simGyro.getRate() * 0.02)
