"""
C++ bindings for photonlib
"""
from __future__ import annotations
__all__ = ['PhotonPipelineMetadata', 'print_t']
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
def print_t(arg0: ...) -> None:
    """
    Print a frc Translation3d
    """
