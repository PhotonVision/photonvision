###############################################################################
## Copyright (C) Photon Vision.
###############################################################################
## This program is free software: you can redistribute it and/or modify
## it under the terms of the GNU General Public License as published by
## the Free Software Foundation, either version 3 of the License, or
## (at your option) any later version.
##
## This program is distributed in the hope that it will be useful,
## but WITHOUT ANY WARRANTY; without even the implied warranty of
## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
## GNU General Public License for more details.
##
## You should have received a copy of the GNU General Public License
## along with this program.  If not, see <https://www.gnu.org/licenses/>.
###############################################################################

from enum import Enum
from typing import List
import ntcore
from wpilib import RobotController, Timer
import wpilib
from .packet import Packet
from .targeting.photonPipelineResult import PhotonPipelineResult
from .version import PHOTONVISION_VERSION, PHOTONLIB_VERSION  # type: ignore[import-untyped]

# magical import to make serde stuff work
import photonlibpy.generated  # noqa


class VisionLEDMode(Enum):
    kDefault = -1
    kOff = 0
    kOn = 1
    kBlink = 2


_lastVersionTimeCheck = 0.0
_VERSION_CHECK_ENABLED = True


def setVersionCheckEnabled(enabled: bool):
    global _VERSION_CHECK_ENABLED
    _VERSION_CHECK_ENABLED = enabled


class PhotonCamera:
    def __init__(self, cameraName: str):
        instance = ntcore.NetworkTableInstance.getDefault()
        self._name = cameraName
        self._tableName = "photonvision"
        photonvision_root_table = instance.getTable(self._tableName)
        self._cameraTable = photonvision_root_table.getSubTable(cameraName)
        self._path = self._cameraTable.getPath()
        self._rawBytesEntry = self._cameraTable.getRawTopic("rawBytes").subscribe(
            f"photonstruct:PhotonPipelineResult:{PhotonPipelineResult.photonStruct.MESSAGE_VERSION}",
            bytes([]),
            ntcore.PubSubOptions(periodic=0.01, sendAll=True),
        )

        self._driverModePublisher = self._cameraTable.getBooleanTopic(
            "driverModeRequest"
        ).publish()
        self._driverModeSubscriber = self._cameraTable.getBooleanTopic(
            "driverMode"
        ).subscribe(False)
        self._inputSaveImgEntry = self._cameraTable.getIntegerTopic(
            "inputSaveImgCmd"
        ).getEntry(0)
        self._outputSaveImgEntry = self._cameraTable.getIntegerTopic(
            "outputSaveImgCmd"
        ).getEntry(0)
        self._pipelineIndexRequest = self._cameraTable.getIntegerTopic(
            "pipelineIndexRequest"
        ).publish()
        self._pipelineIndexState = self._cameraTable.getIntegerTopic(
            "pipelineIndexState"
        ).subscribe(0)
        self._heartbeatEntry = self._cameraTable.getIntegerTopic("heartbeat").subscribe(
            -1
        )

        self._ledModeRequest = photonvision_root_table.getIntegerTopic(
            "ledModeRequest"
        ).publish()
        self._ledModeState = photonvision_root_table.getIntegerTopic(
            "ledModeState"
        ).subscribe(-1)
        self.versionEntry = photonvision_root_table.getStringTopic("version").subscribe(
            ""
        )

        # Existing is enough to make this multisubscriber do its thing
        self.topicNameSubscriber = ntcore.MultiSubscriber(
            instance, ["/photonvision/"], ntcore.PubSubOptions(topicsOnly=True)
        )

        self._prevHeartbeat = 0
        self._prevHeartbeatChangeTime = Timer.getFPGATimestamp()

    def getAllUnreadResults(self) -> List[PhotonPipelineResult]:
        """
        The list of pipeline results sent by PhotonVision since the last call to getAllUnreadResults().
        Calling this function clears the internal FIFO queue, and multiple calls to
        getAllUnreadResults() will return different (potentially empty) result arrays. Be careful to
        call this exactly ONCE per loop of your robot code! FIFO depth is limited to 20 changes, so
        make sure to call this frequently enough to avoid old results being discarded, too!
        """

        self._versionCheck()

        changes = self._rawBytesEntry.readQueue()

        ret = []

        for change in changes:
            byteList = change.value
            timestamp = change.time

            if len(byteList) < 1:
                pass
            else:
                newResult = PhotonPipelineResult()
                pkt = Packet(byteList)
                newResult = PhotonPipelineResult.photonStruct.unpack(pkt)
                # NT4 allows us to correct the timestamp based on when the message was sent
                newResult.ntReceiveTimestampMicros = timestamp
                ret.append(newResult)

        return ret

    def getLatestResult(self) -> PhotonPipelineResult:
        self._versionCheck()

        now = RobotController.getFPGATime()
        packetWithTimestamp = self._rawBytesEntry.getAtomic()
        byteList = packetWithTimestamp.value
        packetWithTimestamp.time

        if len(byteList) < 1:
            return PhotonPipelineResult()
        else:
            pkt = Packet(byteList)
            retVal = PhotonPipelineResult.photonStruct.unpack(pkt)
            # We don't trust NT4 time, hack around
            retVal.ntReceiveTimestampMicros = now
            return retVal

    def getDriverMode(self) -> bool:
        return self._driverModeSubscriber.get()

    def setDriverMode(self, driverMode: bool) -> None:
        self._driverModePublisher.set(driverMode)

    def takeInputSnapshot(self) -> None:
        self._inputSaveImgEntry.set(self._inputSaveImgEntry.get() + 1)

    def takeOutputSnapshot(self) -> None:
        self._outputSaveImgEntry.set(self._outputSaveImgEntry.get() + 1)

    def getPipelineIndex(self) -> int:
        return self._pipelineIndexState.get(0)

    def setPipelineIndex(self, index: int) -> None:
        self._pipelineIndexRequest.set(index)

    def getLEDMode(self) -> VisionLEDMode:
        mode = self._ledModeState.get()
        return VisionLEDMode(mode)

    def setLEDMode(self, led: VisionLEDMode) -> None:
        self._ledModeRequest.set(led.value)

    def getName(self) -> str:
        return self._name

    def isConnected(self) -> bool:
        curHeartbeat = self._heartbeatEntry.get()
        now = Timer.getFPGATimestamp()

        if curHeartbeat != self._prevHeartbeat:
            self._prevHeartbeat = curHeartbeat
            self._prevHeartbeatChangeTime = now

        return (now - self._prevHeartbeatChangeTime) < 0.5

    def _versionCheck(self) -> None:
        global _lastVersionTimeCheck

        if not _VERSION_CHECK_ENABLED:
            return

        if (Timer.getFPGATimestamp() - _lastVersionTimeCheck) < 5.0:
            return

        _lastVersionTimeCheck = Timer.getFPGATimestamp()

        if not self._heartbeatEntry.exists():
            cameraNames = (
                self._cameraTable.getInstance().getTable(self._tableName).getSubTables()
            )
            # Look for only cameras with rawBytes entry that exists
            cameraNames = list(
                filter(
                    lambda it: self._cameraTable.getSubTable(it)
                    .getEntry("rawBytes")
                    .exists(),
                    cameraNames,
                )
            )

            if len(cameraNames) == 0:
                wpilib.reportError(
                    "Could not find any PhotonVision coprocessors on NetworkTables. Double check that PhotonVision is running, and that your camera is connected!",
                    False,
                )
            else:
                wpilib.reportError(
                    f"PhotonVision coprocessor at path {self._path} not found in Network Tables. Double check that your camera names match! Only the following camera names were found: { ''.join(cameraNames)}",
                    True,
                )

        elif not self.isConnected():
            wpilib.reportWarning(
                f"PhotonVision coprocessor at path {self._path} is not sending new data.",
                True,
            )

        versionString = self.versionEntry.get(defaultValue="")
        localUUID = PhotonPipelineResult.photonStruct.MESSAGE_VERSION

        remoteUUID = self._rawBytesEntry.getTopic().getProperty("message_uuid")

        if remoteUUID is None or len(remoteUUID) == 0:
            wpilib.reportWarning(
                f"PhotonVision coprocessor at path {self._path} has not reported a message interface UUID - is your coprocessor's camera started?",
                True,
            )

        # ntcore hands us a JSON string with leading/trailing quotes - remove those
        remoteUUID = remoteUUID.replace('"', "")

        if localUUID != remoteUUID:
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

            errText = f"Photonlibpy version {PHOTONLIB_VERSION} (With message UUID {localUUID}) does not match coprocessor version {versionString} (with message UUID {remoteUUID}). Please install photonlibpy version {versionString}, or update your coprocessor to {PHOTONLIB_VERSION}."
            wpilib.reportError(errText, True)
            raise Exception(errText)
