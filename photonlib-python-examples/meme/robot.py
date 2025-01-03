#!/usr/bin/env python3
"""
    This is a good foundation to build your robot code on
"""

import wpilib
import wpilib.drive
from photonlibpy import PhotonCamera
from wpilib import SmartDashboard

class MyRobot(wpilib.TimedRobot):

    def __init__(self):
        wpilib.TimedRobot.__init__(self)
        self.benchyLight = wpilib.DigitalOutput(0)
        self.curState = False
        self.lastTransitionTime = wpilib.Timer.getFPGATimestamp()
        self.blinkHalfPeriod = 0.5

        self.benchyLight.set(False)

        self.camera = PhotonCamera("Arducam_OV9782_USB_Camera")

    def disabledPeriodic(self) -> None:
        curTime = wpilib.Timer.getFPGATimestamp()

        if(curTime > self.lastTransitionTime + self.blinkHalfPeriod):
            self.curState = not self.curState
            self.lastTransitionTime = curTime

            if self.curState:
                self.should_listen = True

        self.benchyLight.set(self.curState)
        
    def robotPeriodic(self):
        super().robotPeriodic()

        results = self.camera.getAllUnreadResults()
        if self.should_listen:
            for r in results:
                if r.hasTargets():
                    # Found!
                    latency = wpilib.Timer.getFPGATimestamp() - self.lastTransitionTime
                    SmartDashboard.putNumber("latency", latency)

                    self.should_listen = False



if __name__ == "__main__":
    wpilib.run(MyRobot)
