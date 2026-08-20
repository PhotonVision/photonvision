"""Test utilities."""

from photonlibpy.targeting import PhotonPipelineMetadata


class InvalidTestDataException(ValueError):
    pass


class PipelineTimestamps:
    """Helper class to ensure timestamps are positive."""

    def __init__(
        self,
        *,
        captureTimestampMicros: int,
        pipelineLatencyMicros=2_000,
    ):
        if captureTimestampMicros < 0:
            raise InvalidTestDataException("captureTimestampMicros cannot be negative")
        if pipelineLatencyMicros <= 0:
            raise InvalidTestDataException("pipelineLatencyMicros must be positive")
        self._captureTimestampMicros = captureTimestampMicros
        self._pipelineLatencyMicros = pipelineLatencyMicros
        self._sequenceID = 0

    @property
    def captureTimestampMicros(self) -> int:
        return self._captureTimestampMicros

    @captureTimestampMicros.setter
    def captureTimestampMicros(self, micros: int) -> None:
        if micros < 0:
            raise InvalidTestDataException("captureTimestampMicros cannot be negative")
        if micros < self._captureTimestampMicros:
            raise InvalidTestDataException("time cannot go backwards")
        self._captureTimestampMicros = micros
        self._sequenceID += 1

    @property
    def pipelineLatencyMicros(self) -> int:
        return self._pipelineLatencyMicros

    def pipelineLatencySecs(self) -> float:
        return self.pipelineLatencyMicros * 1e-6

    def incrementTimeMicros(self, micros: int) -> None:
        self.captureTimestampMicros += micros

    def publishTimestampMicros(self) -> int:
        return self._captureTimestampMicros + self.pipelineLatencyMicros

    def toPhotonPipelineMetadata(self) -> PhotonPipelineMetadata:
        return PhotonPipelineMetadata(
            captureTimestampMicros=self.captureTimestampMicros,
            publishTimestampMicros=self.publishTimestampMicros(),
            sequenceID=self._sequenceID,
        )
