"""
C++ bindings for photonlib
"""
from __future__ import annotations
from wpimath.geometry import Translation3d
__all__ = ['Translation3d', 'print_t']
def print_t(t: Translation3d) -> None:
    """
    Print an frc::Translation3d
    """
