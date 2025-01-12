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

import wpilib
import wpilib.simulation
import wpimath.controller
import wpimath.filter
import wpimath.geometry
import wpimath.kinematics
import wpimath.trajectory
import wpimath.units

kWheelRadius = 0.0508
kEncoderResolution = 4096
kModuleMaxAngularVelocity = math.pi
kModuleMaxAngularAcceleration = math.tau


class SwerveModule:
    def __init__(
        self,
        driveMotorChannel: int,
        turningMotorChannel: int,
        driveEncoderChannelA: int,
        driveEncoderChannelB: int,
        turningEncoderChannelA: int,
        turningEncoderChannelB: int,
        moduleNumber: int,
    ) -> None:
        """Constructs a SwerveModule with a drive motor, turning motor, drive encoder and turning encoder.

        :param driveMotorChannel:      PWM output for the drive motor.
        :param turningMotorChannel:    PWM output for the turning motor.
        :param driveEncoderChannelA:   DIO input for the drive encoder channel A
        :param driveEncoderChannelB:   DIO input for the drive encoder channel B
        :param turningEncoderChannelA: DIO input for the turning encoder channel A
        :param turningEncoderChannelB: DIO input for the turning encoder channel B
        """
        self.moduleNumber = moduleNumber
        self.desiredState = wpimath.kinematics.SwerveModuleState()

        self.driveMotor = wpilib.PWMSparkMax(driveMotorChannel)
        self.turningMotor = wpilib.PWMSparkMax(turningMotorChannel)

        self.driveEncoder = wpilib.Encoder(driveEncoderChannelA, driveEncoderChannelB)
        self.turningEncoder = wpilib.Encoder(
            turningEncoderChannelA, turningEncoderChannelB
        )

        # Gains are for example purposes only - must be determined for your own robot!
        self.drivePIDController = wpimath.controller.PIDController(10, 0, 0)

        # Gains are for example purposes only - must be determined for your own robot!
        self.turningPIDController = wpimath.controller.PIDController(30, 0, 0)

        # Gains are for example purposes only - must be determined for your own robot!
        self.driveFeedforward = wpimath.controller.SimpleMotorFeedforwardMeters(1, 3)

        # Set the distance per pulse for the drive encoder. We can simply use the
        # distance traveled for one rotation of the wheel divided by the encoder
        # resolution.
        self.driveEncoder.setDistancePerPulse(
            math.tau * kWheelRadius / kEncoderResolution
        )

        # Set the distance (in this case, angle) in radians per pulse for the turning encoder.
        # This is the the angle through an entire rotation (2 * pi) divided by the
        # encoder resolution.
        self.turningEncoder.setDistancePerPulse(math.tau / kEncoderResolution)

        # Limit the PID Controller's input range between -pi and pi and set the input
        # to be continuous.
        self.turningPIDController.enableContinuousInput(-math.pi, math.pi)

        # Simulation Support
        self.simDriveEncoder = wpilib.simulation.EncoderSim(self.driveEncoder)
        self.simTurningEncoder = wpilib.simulation.EncoderSim(self.turningEncoder)
        self.simDrivingMotor = wpilib.simulation.PWMSim(self.driveMotor)
        self.simTurningMotor = wpilib.simulation.PWMSim(self.turningMotor)
        self.simDrivingMotorFilter = wpimath.filter.LinearFilter.singlePoleIIR(
            0.1, 0.02
        )
        self.simTurningMotorFilter = wpimath.filter.LinearFilter.singlePoleIIR(
            0.0001, 0.02
        )
        self.simTurningEncoderPos = 0
        self.simDrivingEncoderPos = 0

    def getState(self) -> wpimath.kinematics.SwerveModuleState:
        """Returns the current state of the module.

        :returns: The current state of the module.
        """
        return wpimath.kinematics.SwerveModuleState(
            self.driveEncoder.getRate(),
            wpimath.geometry.Rotation2d(self.turningEncoder.getDistance()),
        )

    def getPosition(self) -> wpimath.kinematics.SwerveModulePosition:
        """Returns the current position of the module.

        :returns: The current position of the module.
        """
        return wpimath.kinematics.SwerveModulePosition(
            self.driveEncoder.getDistance(),
            wpimath.geometry.Rotation2d(self.turningEncoder.getDistance()),
        )

    def setDesiredState(
        self, desiredState: wpimath.kinematics.SwerveModuleState
    ) -> None:
        """Sets the desired state for the module.

        :param desiredState: Desired state with speed and angle.
        """
        self.desiredState = desiredState

        encoderRotation = wpimath.geometry.Rotation2d(self.turningEncoder.getDistance())

        # Optimize the reference state to avoid spinning further than 90 degrees
        self.desiredState.optimize(encoderRotation)

        # Scale speed by cosine of angle error. This scales down movement perpendicular to the desired
        # direction of travel that can occur when modules change directions. This results in smoother
        # driving.
        self.desiredState.speed *= (self.desiredState.angle - encoderRotation).cos()

        # Calculate the drive output from the drive PID controller.
        driveOutput = self.drivePIDController.calculate(
            self.driveEncoder.getRate(), self.desiredState.speed
        )

        driveFeedforward = self.driveFeedforward.calculate(self.desiredState.speed)

        # Calculate the turning motor output from the turning PID controller.
        turnOutput = self.turningPIDController.calculate(
            self.turningEncoder.getDistance(), self.desiredState.angle.radians()
        )

        self.driveMotor.setVoltage(driveOutput + driveFeedforward)
        self.turningMotor.setVoltage(turnOutput)

    def getAbsoluteHeading(self) -> wpimath.geometry.Rotation2d:
        return wpimath.geometry.Rotation2d(self.turningEncoder.getDistance())

    def log(self) -> None:
        state = self.getState()

        table = "Module " + str(self.moduleNumber) + "/"
        wpilib.SmartDashboard.putNumber(
            table + "Steer Degrees",
            math.degrees(wpimath.angleModulus(state.angle.radians())),
        )
        wpilib.SmartDashboard.putNumber(
            table + "Steer Target Degrees",
            math.degrees(self.turningPIDController.getSetpoint()),
        )
        wpilib.SmartDashboard.putNumber(table + "Drive Velocity Feet", state.speed_fps)
        wpilib.SmartDashboard.putNumber(
            table + "Drive Velocity Target Feet", self.desiredState.speed_fps
        )
        wpilib.SmartDashboard.putNumber(
            table + "Drive Voltage", self.driveMotor.get() * 12.0
        )
        wpilib.SmartDashboard.putNumber(
            table + "Steer Voltage", self.turningMotor.get() * 12.0
        )

    def simulationPeriodic(self) -> None:
        driveVoltage = (
            self.simDrivingMotor.getSpeed() * wpilib.RobotController.getBatteryVoltage()
        )
        turnVoltage = (
            self.simTurningMotor.getSpeed() * wpilib.RobotController.getBatteryVoltage()
        )
        driveSpdRaw = (
            driveVoltage / 12.0 * self.driveFeedforward.maxAchievableVelocity(12.0, 0)
        )
        turnSpdRaw = turnVoltage / 0.7
        driveSpd = self.simDrivingMotorFilter.calculate(driveSpdRaw)
        turnSpd = self.simTurningMotorFilter.calculate(turnSpdRaw)
        self.simDrivingEncoderPos += 0.02 * driveSpd
        self.simTurningEncoderPos += 0.02 * turnSpd
        self.simDriveEncoder.setDistance(self.simDrivingEncoderPos)
        self.simDriveEncoder.setRate(driveSpd)
        self.simTurningEncoder.setDistance(self.simTurningEncoderPos)
        self.simTurningEncoder.setRate(turnSpd)
