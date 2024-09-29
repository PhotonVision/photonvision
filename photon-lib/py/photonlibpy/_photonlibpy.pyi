"""
C++ bindings for photonlib
"""
from __future__ import annotations
from wpimath.geometry import Translation3d
__all__ = ['PhotonPipelineMetadata', 'Translation3d', 'print_t']
class PhotonPipelineMetadata:
    def __init__(self) -> None:
        ...
    @property
    def captureTimestampMicros(self) -> int:
        ...
    @property
    def publishTimestampMicros(self) -> int:
        ...
    @property
    def sequenceID(self) -> int:
        ...
def print_t(t: Translation3d) -> None:
    """
    hello
    """
