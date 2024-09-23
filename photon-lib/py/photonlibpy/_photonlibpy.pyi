"""
C++ bindings for photonlib
"""
from __future__ import annotations
__all__ = ['PhotonPipelineMetadata']
class PhotonPipelineMetadata:
    @property
    def captureTimestampMicros(self) -> int:
        ...
    @property
    def publishTimestampMicros(self) -> int:
        ...
    @property
    def sequenceID(self) -> int:
        ...
