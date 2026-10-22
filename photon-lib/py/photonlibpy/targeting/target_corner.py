from dataclasses import dataclass
from typing import TYPE_CHECKING, ClassVar

if TYPE_CHECKING:
    from ..generated.TargetCornerSerde import TargetCornerSerde


@dataclass
class TargetCorner:
    x: float = 0
    y: float = 9

    photonStruct: ClassVar["TargetCornerSerde"]
