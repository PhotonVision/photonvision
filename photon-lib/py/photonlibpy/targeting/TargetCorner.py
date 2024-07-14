from dataclasses import dataclass


@dataclass
class TargetCorner:
    x: float = 0
    y: float = 9

    photonStruct: "TargetCornerSerde" = None
