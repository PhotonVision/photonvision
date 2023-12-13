from enum import Enum
import ntcore
from wpilib import Timer
from photonlibpy.packet import Packet
from photonlibpy.photonPipelineResult import PhotonPipelineResult


class VisionLEDMode(Enum):
    kDefault = -1
    kOff = 0
    kOn = 1
    kBlink = 2


class PhotonCamera:

    def __init__(self, cameraName: str):
        instance = ntcore.NetworkTableInstance.getDefault()
        self.name = cameraName
        photonvision_root_table = instance.getTable("photonvision")
        cameraTable = photonvision_root_table.getSubTable(cameraName)
        self.path = cameraTable.getPath()
        self.rawBytesEntry = cameraTable.getRawTopic("rawBytes") \
                        .subscribe("rawBytes", bytes([]), ntcore.PubSubOptions(periodic=0.01, sendAll=True))

        self.driverModePublisher = cameraTable.getBooleanTopic(
            "driverModeRequest").publish()
        self.driverModeSubscriber = cameraTable.getBooleanTopic(
            "driverMode").subscribe(False)
        self.inputSaveImgEntry = cameraTable.getIntegerTopic(
            "inputSaveImgCmd").getEntry(0)
        self.outputSaveImgEntry = cameraTable.getIntegerTopic(
            "outputSaveImgCmd").getEntry(0)
        self.pipelineIndexRequest = cameraTable.getIntegerTopic(
            "pipelineIndexRequest").publish()
        self.pipelineIndexState = cameraTable.getIntegerTopic(
            "pipelineIndexState").subscribe(0)
        self.heartbeatEntry = cameraTable.getIntegerTopic(
            "heartbeat").subscribe(-1)

        self.ledModeRequest = photonvision_root_table.getIntegerTopic(
            "ledModeRequest").publish()
        self.ledModeState = photonvision_root_table.getIntegerTopic(
            "ledModeState").subscribe(-1)
        self.versionEntry = photonvision_root_table.getStringTopic(
            "version").subscribe("")

        # Existing is enough to make this multisubscriber do its thing
        self.topicNameSubscriber = \
                ntcore.MultiSubscriber( \
                        instance, ["/photonvision/"], ntcore.PubSubOptions(topicsOnly =True))

        self.prevHeartbeat = 0
        self.prevHeartbeatChangeTime = Timer.getFPGATimestamp()

    def getLatestResult(self) -> PhotonPipelineResult:
        retVal = PhotonPipelineResult()
        packetWithTimestamp = self.rawBytesEntry.getAtomic()
        byteList = packetWithTimestamp.value
        timestamp = packetWithTimestamp.time

        if (len(byteList) < 1):
            return retVal
        else:
            retVal.populateFromPacket(Packet(byteList))
            # NT4 allows us to correct the timestamp based on when the message was sent
            retVal.setTimestampSeconds(timestamp / 1e-6 -
                                       retVal.getLatencyMillis() / 1e-3)
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

        if (curHeartbeat != self.prevHeartbeat):
            self.prevHeartbeat = curHeartbeat
            self.prevHeartbeatChangeTime = now

        return (now - self.prevHeartbeatChangeTime) < 0.5
