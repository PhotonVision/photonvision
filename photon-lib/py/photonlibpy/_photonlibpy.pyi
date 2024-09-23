"""
C++ bindings for photonlib
"""
from __future__ import annotations
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
class Translation3d:
    def __init__(self, arg0: float, arg1: float, arg2: float) -> None:
        ...
def print_t(arg0: Translation3d) -> None:
    """
    Print a frc Translation3d
    """
