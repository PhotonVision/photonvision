
import wpilib
from photonlibpy import PhotonCamera, version


class MyRobot(wpilib.TimedRobot):
    def robotInit(self) -> None:
        self.cam = PhotonCamera("memes")
    
    def robotPeriodic(self) -> None:
        pass

    def teleopPeriodic(self) -> None:
        print("memes")
        print(version.PHOTONLIB_VERSION)
