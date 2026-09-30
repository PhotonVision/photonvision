"""Test utilities."""

from photonlibpy.targeting import PhotonPipelineMetadata


class InvalidTestDataException(ValueError):
    pass


class PipelineTimestamps:
    """Helper class to ensure timestamps are positive."""

    def __init__(
        self,
        *,
        captureTimestampNanos: int,
        pipelineLatencyNanos=2e6,
        receiveLatencyNanos=1e6,
    ):
        if captureTimestampNanos < 0:
            raise InvalidTestDataException("captureTimestampNanos cannot be negative")
        if pipelineLatencyNanos <= 0:
            raise InvalidTestDataException("pipelineLatencyNanos must be positive")
        if receiveLatencyNanos < 0:
            raise InvalidTestDataException("receiveLatencyNanos cannot be negative")
        self._captureTimestampNanos = captureTimestampNanos
        self._pipelineLatencyNanos = pipelineLatencyNanos
        self._receiveLatencyNanos = receiveLatencyNanos
        self._sequenceID = 0

    @property
    def captureTimestampNanos(self) -> int:
        return self._captureTimestampNanos

    @captureTimestampNanos.setter
    def captureTimestampNanos(self, nanos: int) -> None:
        if nanos < 0:
            raise InvalidTestDataException("captureTimestampNanos cannot be negative")
        if nanos < self._captureTimestampNanos:
            raise InvalidTestDataException("time cannot go backwards")
        self._captureTimestampNanos = nanos
        self._sequenceID += 1

    @property
    def pipelineLatencyNanos(self) -> int:
        return self._pipelineLatencyNanos

    def pipelineLatencySecs(self) -> float:
        return self.pipelineLatencyNanos * 1e-9

    def incrementTimeNanos(self, nanos: int) -> None:
        self.captureTimestampNanos += nanos

    def publishTimestampNanos(self) -> int:
        return self._captureTimestampNanos + self.pipelineLatencyNanos

    def receiveTimestampNanos(self) -> int:
        return self.publishTimestampNanos() + self._receiveLatencyNanos

    def toPhotonPipelineMetadata(self) -> PhotonPipelineMetadata:
        return PhotonPipelineMetadata(
            captureTimestampNanos=self.captureTimestampNanos,
            publishTimestampNanos=self.publishTimestampNanos(),
            sequenceID=self._sequenceID,
        )
