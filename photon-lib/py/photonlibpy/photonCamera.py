from enum import Enum
import ntcore
from wpilib import Timer
import wpilib
from photonlibpy.packet import Packet
from photonlibpy.photonPipelineResult import PhotonPipelineResult
from photonlibpy.version import PHOTONVISION_VERSION, PHOTONLIB_VERSION


class VisionLEDMode(Enum):
    kDefault = -1
    kOff = 0
    kOn = 1
    kBlink = 2


lastVersionTimeCheck = 0.0
_VERSION_CHECK_ENABLED = True


def setVersionCheckEnabled(enabled: bool):
    global _VERSION_CHECK_ENABLED
    _VERSION_CHECK_ENABLED = enabled


class PhotonCamera:
    def __init__(self, cameraName: str):
        instance = ntcore.NetworkTableInstance.getDefault()
        self.name = cameraName
        self._tableName = "photonvision"
        photonvision_root_table = instance.getTable(self._tableName)
        self.cameraTable = photonvision_root_table.getSubTable(cameraName)
        self.path = self.cameraTable.getPath()
        self.rawBytesEntry = self.cameraTable.getRawTopic("rawBytes").subscribe(
            "rawBytes", bytes([]), ntcore.PubSubOptions(periodic=0.01, sendAll=True)
        )

        self.driverModePublisher = self.cameraTable.getBooleanTopic(
            "driverModeRequest"
        ).publish()
        self.driverModeSubscriber = self.cameraTable.getBooleanTopic(
            "driverMode"
        ).subscribe(False)
        self.inputSaveImgEntry = self.cameraTable.getIntegerTopic(
            "inputSaveImgCmd"
        ).getEntry(0)
        self.outputSaveImgEntry = self.cameraTable.getIntegerTopic(
            "outputSaveImgCmd"
        ).getEntry(0)
        self.pipelineIndexRequest = self.cameraTable.getIntegerTopic(
            "pipelineIndexRequest"
        ).publish()
        self.pipelineIndexState = self.cameraTable.getIntegerTopic(
            "pipelineIndexState"
        ).subscribe(0)
        self.heartbeatEntry = self.cameraTable.getIntegerTopic("heartbeat").subscribe(
            -1
        )

        self.ledModeRequest = photonvision_root_table.getIntegerTopic(
            "ledModeRequest"
        ).publish()
        self.ledModeState = photonvision_root_table.getIntegerTopic(
            "ledModeState"
        ).subscribe(-1)
        self.versionEntry = photonvision_root_table.getStringTopic("version").subscribe(
            ""
        )

        # Existing is enough to make this multisubscriber do its thing
        self.topicNameSubscriber = ntcore.MultiSubscriber(
            instance, ["/photonvision/"], ntcore.PubSubOptions(topicsOnly=True)
        )

        self.prevHeartbeat = 0
        self.prevHeartbeatChangeTime = Timer.getFPGATimestamp()

    def getLatestResult(self) -> PhotonPipelineResult:
        self._versionCheck()

        retVal = PhotonPipelineResult()
        packetWithTimestamp = self.rawBytesEntry.getAtomic()
        byteList = packetWithTimestamp.value
        timestamp = packetWithTimestamp.time

        if len(byteList) < 1:
            return retVal
        else:
            retVal.populateFromPacket(Packet(byteList))
            # NT4 allows us to correct the timestamp based on when the message was sent
            retVal.setTimestampSeconds(
                timestamp / 1e-6 - retVal.getLatencyMillis() / 1e-3
            )
            return retVal

    def getDriverMode(self) -> bool:
        return self.driverModeSubscriber.get()

    def setDriverMode(self, driverMode: bool) -> None:
        self.driverModePublisher.set(driverMode)

    def takeInputSnapshot(self) -> None:
        self.inputSaveImgEntry.set(self.inputSaveImgEntry.get() + 1)

    def takeOutputSnapshot(self) -> None:
        self.outputSaveImgEntry.set(self.outputSaveImgEntry.get() + 1)

    def getPipelineIndex(self) -> int:
        return self.pipelineIndexState.get(0)

    def setPipelineIndex(self, index: int) -> None:
        self.pipelineIndexRequest.set(index)

    def getLEDMode(self) -> VisionLEDMode:
        mode = self.ledModeState.get()
        return VisionLEDMode(mode)

    def setLEDMode(self, led: VisionLEDMode) -> None:
        self.ledModeRequest.set(led.value)

    def getName(self) -> str:
        return self.name

    def isConnected(self) -> bool:
        curHeartbeat = self.heartbeatEntry.get()
        now = Timer.getFPGATimestamp()

        if curHeartbeat != self.prevHeartbeat:
            self.prevHeartbeat = curHeartbeat
            self.prevHeartbeatChangeTime = now

        return (now - self.prevHeartbeatChangeTime) < 0.5

    def _versionCheck(self) -> None:
        global lastVersionTimeCheck

        if not _VERSION_CHECK_ENABLED:
            return

        if (Timer.getFPGATimestamp() - lastVersionTimeCheck) < 5.0:
            return

        lastVersionTimeCheck = Timer.getFPGATimestamp()

        if not self.heartbeatEntry.exists():
            cameraNames = (
                self.cameraTable.getInstance().getTable(self._tableName).getSubTables()
            )
            if len(cameraNames) == 0:
                wpilib.reportError(
                    "Could not find any PhotonVision coprocessors on NetworkTables. Double check that PhotonVision is running, and that your camera is connected!",
                    False,
                )
            else:
                wpilib.reportError(
                    f"PhotonVision coprocessor at path {self.path} not found in Network Tables. Double check that your camera names match! Only the following camera names were found: { ''.join(cameraNames)}",
                    True,
                )

        elif not self.isConnected():
            wpilib.reportWarning(
                f"PhotonVision coprocessor at path {self.path} is not sending new data.",
                True,
            )

        versionString = self.versionEntry.get(defaultValue="")
        if len(versionString) > 0 and versionString != PHOTONVISION_VERSION:
            # Verified version mismatch

            bfw = """
            \n\n\n
            >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
            >>> !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            >>>
            >>> You are running an incompatible version
            >>> of PhotonVision on your coprocessor!
            >>>
            >>> This is neither tested nor supported.
            >>> You MUST update PhotonVision,
            >>> PhotonLib, or both.
            >>>
            >>> Your code will now crash.
            >>> We hope your day gets better.
            >>>
            >>> !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
            \n\n
            """

            wpilib.reportWarning(bfw)

            errText = f"Photon version {PHOTONLIB_VERSION} does not match coprocessor version {versionString}. Please install photonlibpy version {PHOTONLIB_VERSION}."
            wpilib.reportError(errText, True)
            raise Exception(errText)
