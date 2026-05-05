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
import wpimath

kMaxSpeed = 3.0  # 3 meters per second
kMaxAngularSpeed = math.pi  # 1/2 rotation per second


class Drivetrain:
    """
    Represents a swerve drive style drivetrain.
    """

    def __init__(self) -> None:
        self.frontLeftLocation = wpimath.Translation2d(0.381, 0.381)
        self.frontRightLocation = wpimath.Translation2d(0.381, -0.381)
        self.backLeftLocation = wpimath.Translation2d(-0.381, 0.381)
        self.backRightLocation = wpimath.Translation2d(-0.381, -0.381)

        self.frontLeft = swervemodule.SwerveModule(1, 2, 0, 1, 2, 3, 1)
        self.frontRight = swervemodule.SwerveModule(3, 4, 4, 5, 6, 7, 2)
        self.backLeft = swervemodule.SwerveModule(5, 6, 8, 9, 10, 11, 3)
        self.backRight = swervemodule.SwerveModule(7, 8, 12, 13, 14, 15, 4)

        self.debugField = wpilib.Field2d()
        wpilib.SmartDashboard.putData("Drivetrain Debug", self.debugField)

        self.gyro = wpilib.AnalogGyro(0)
        self.simGyro = wpilib.simulation.AnalogGyroSim(self.gyro)

        self.kinematics = wpimath.SwerveDrive4Kinematics(
            self.frontLeftLocation,
            self.frontRightLocation,
            self.backLeftLocation,
            self.backRightLocation,
        )

        self.odometry = wpimath.SwerveDrive4Odometry(
            self.kinematics,
            self.gyro.getRotation2d(),
            (
                self.frontLeft.getPosition(),
                self.frontRight.getPosition(),
                self.backLeft.getPosition(),
                self.backRight.getPosition(),
            ),
        )

        self.targetChassisVelocities = wpimath.ChassisVelocities()

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
        chassisVelocities = wpimath.ChassisVelocities(xSpeed, ySpeed, rot)
        if fieldRelative:
            chassisVelocities = chassisVelocities.toRobotRelative(
                self.gyro.getRotation2d()
            )

        chassisVelocities = chassisVelocities.discretize(periodSeconds)
        swerveModuleStates = self.kinematics.desaturateWheelVelocities(
            self.kinematics.toSwerveModuleVelocities(chassisVelocities), kMaxSpeed
        )
        self.frontLeft.setDesiredVelocity(swerveModuleStates[0])
        self.frontRight.setDesiredVelocity(swerveModuleStates[1])
        self.backLeft.setDesiredVelocity(swerveModuleStates[2])
        self.backRight.setDesiredVelocity(swerveModuleStates[3])

        self.targetChassisVelocities = self.kinematics.toChassisVelocities(
            swerveModuleStates
        )

    def updateOdometry(self) -> None:
        """Updates the field relative position of the robot."""
        self.odometry.update(
            self.gyro.getRotation2d(),
            (
                self.frontLeft.getPosition(),
                self.frontRight.getPosition(),
                self.backLeft.getPosition(),
                self.backRight.getPosition(),
            ),
        )

    def getModuleVelocities(self) -> list[wpimath.SwerveModuleVelocity]:
        return [
            self.frontLeft.getVelocity(),
            self.frontRight.getVelocity(),
            self.backLeft.getVelocity(),
            self.backRight.getVelocity(),
        ]

    def getModulePoses(self) -> list[wpimath.Pose2d]:
        p = self.odometry.getPose()
        flTrans = wpimath.Transform2d(
            self.frontLeftLocation, self.frontLeft.getAbsoluteHeading()
        )
        frTrans = wpimath.Transform2d(
            self.frontRightLocation, self.frontRight.getAbsoluteHeading()
        )
        blTrans = wpimath.Transform2d(
            self.backLeftLocation, self.backLeft.getAbsoluteHeading()
        )
        brTrans = wpimath.Transform2d(
            self.backRightLocation, self.backRight.getAbsoluteHeading()
        )
        return [
            p.transformBy(flTrans),
            p.transformBy(frTrans),
            p.transformBy(blTrans),
            p.transformBy(brTrans),
        ]

    def getChassisVelocities(self) -> wpimath.ChassisVelocities:
        return self.kinematics.toChassisVelocities(self.getModuleVelocities())

    def log(self):
        table = "Drive/"

        pose = self.odometry.getPose()
        wpilib.SmartDashboard.putNumber(table + "X", pose.X())
        wpilib.SmartDashboard.putNumber(table + "Y", pose.Y())
        wpilib.SmartDashboard.putNumber(table + "Heading", pose.rotation().degrees())

        chassisSpeeds = self.getChassisVelocities()
        wpilib.SmartDashboard.putNumber(table + "VX", chassisSpeeds.vx)
        wpilib.SmartDashboard.putNumber(table + "VY", chassisSpeeds.vy)
        wpilib.SmartDashboard.putNumber(
            table + "Omega Degrees", chassisSpeeds.omega_dps
        )

        wpilib.SmartDashboard.putNumber(
            table + "Target VX", self.targetChassisVelocities.vx
        )
        wpilib.SmartDashboard.putNumber(
            table + "Target VY", self.targetChassisVelocities.vy
        )
        wpilib.SmartDashboard.putNumber(
            table + "Target Omega Degrees", self.targetChassisVelocities.omega_dps
        )

        self.frontLeft.log()
        self.frontRight.log()
        self.backLeft.log()
        self.backRight.log()

        self.debugField.getRobotObject().setPose(self.odometry.getPose())
        self.debugField.getObject("SwerveModules").setPoses(self.getModulePoses())

    def simulationPeriodic(self):
        self.frontLeft.simulationPeriodic()
        self.frontRight.simulationPeriodic()
        self.backLeft.simulationPeriodic()
        self.backRight.simulationPeriodic()
        self.simGyro.setRate(-1.0 * self.getChassisVelocities().omega_dps)
        self.simGyro.setAngle(self.simGyro.getAngle() + self.simGyro.getRate() * 0.02)
