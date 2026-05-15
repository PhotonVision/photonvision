from dataclasses import dataclass
from typing import Optional

from wpimath.geometry import Transform3d

# TODO: Autogenerate python test classes?


@dataclass
class Int8TestMessage:
    test: int
    vlaTest: list[int]
    optTest: Optional[int]


@dataclass
class Int16TestMessage:
    test: int
    vlaTest: list[int]
    optTest: Optional[int]


@dataclass
class Int32TestMessage:
    test: int
    vlaTest: list[int]
    optTest: Optional[int]


@dataclass
class Int64TestMessage:
    test: int
    vlaTest: list[int]
    optTest: Optional[int]


@dataclass
class Float32TestMessage:
    test: float
    vlaTest: list[float]
    optTest: Optional[float]


@dataclass
class Float64TestMessage:
    test: float
    vlaTest: list[float]
    optTest: Optional[float]


@dataclass
class BoolTestMessage:
    test: bool
    vlaTest: list[bool]
    optTest: Optional[bool]


@dataclass
class Transform3dTestMessage:
    test: Transform3d
    vlaTest: list[Transform3d]
    optTest: Optional[Transform3d]
