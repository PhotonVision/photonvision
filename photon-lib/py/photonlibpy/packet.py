###############################################################################
## Copyright (C) Photon Vision.
###############################################################################
## This program is free software: you can redistribute it and/or modify
## it under the terms of the GNU General Public License as published by
## the Free Software Foundation, either version 3 of the License, or
## (at your option) any later version.
##
## This program is distributed in the hope that it will be useful,
## but WITHOUT ANY WARRANTY; without even the implied warranty of
## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
## GNU General Public License for more details.
##
## You should have received a copy of the GNU General Public License
## along with this program.  If not, see <https://www.gnu.org/licenses/>.
###############################################################################

import struct
from typing import Generic, Optional, Protocol, TypeVar

import wpilib
from wpimath.geometry import Quaternion, Rotation3d, Transform3d, Translation3d

T = TypeVar("T")


class Serde(Generic[T], Protocol):
    def pack(self, value: T) -> "Packet": ...
    def unpack(self, packet: "Packet") -> T: ...


class Packet:
    def __init__(self, data: bytes = b""):
        """
        * Constructs an empty packet.
        *
        * @param self.size The self.size of the packet buffer.
        """
        self.packetData = data
        self.size = len(data)
        self.readPos = 0
        self.outOfBytes = False

    def clear(self) -> None:
        """Clears the packet and resets the read and write positions."""
        self.packetData = bytes(self.size)
        self.readPos = 0
        self.outOfBytes = False

    def getSize(self):
        return self.size

    _NO_MORE_BYTES_MESSAGE = """
    Photonlib - Ran out of bytes while decoding.
    Make sure the version of photonvision on the coprocessor
    matches the version of photonlib running in the robot code.
    """

    def _getNextByteAsInt(self) -> int:
        retVal = 0x00

        if not self.outOfBytes:
            try:
                retVal = 0x00FF & self.packetData[self.readPos]
                self.readPos += 1
            except IndexError:
                wpilib.reportError(Packet._NO_MORE_BYTES_MESSAGE, True)
                self.outOfBytes = True

        return retVal

    def getData(self) -> bytes:
        """
        * Returns the packet data.
        *
        * @return The packet data.
        """
        return self.packetData

    def setData(self, data: bytes):
        """
        * Sets the packet data.
        *
        * @param data The packet data.
        """
        self.clear()
        self.packetData = data
        self.size = len(self.packetData)

    def _decodeGeneric(self, unpackFormat, numBytes):
        # Read ints in from the data buffer
        intList = []
        for _ in range(numBytes):
            intList.append(self._getNextByteAsInt())

        # Interpret the bytes as the requested type.
        # Note due to NT's byte order assumptions,
        # we have to flip the order of intList
        value = struct.unpack(unpackFormat, bytes(intList))[0]

        return value

    def decode8(self) -> int:
        """
        * Returns a single decoded byte from the packet.
        *
        * @return A decoded byte from the packet.
        """
        return self._decodeGeneric("<b", 1)

    def decode16(self) -> int:
        """
        * Returns a single decoded short from the packet.
        *
        * @return A decoded short from the packet.
        """
        return self._decodeGeneric("<h", 2)

    def decodeInt(self) -> int:
        """
        * Returns a decoded int (32 bytes) from the packet.
        *
        * @return A decoded int from the packet.
        """
        return self._decodeGeneric("<l", 4)

    def decodeFloat(self) -> float:
        """
        * Returns a decoded float from the packet.
        *
        * @return A decoded float from the packet.
        """
        return self._decodeGeneric("<f", 4)

    def decodeLong(self) -> int:
        """
        * Returns a decoded int64 from the packet.
        *
        * @return A decoded int64 from the packet.
        """
        return self._decodeGeneric("<q", 8)

    def decodeDouble(self) -> float:
        """
        * Returns a decoded double from the packet.
        *
        * @return A decoded double from the packet.
        """
        return self._decodeGeneric("<d", 8)

    def decodeBoolean(self) -> bool:
        """
        * Returns a decoded boolean from the packet.
        *
        * @return A decoded boolean from the packet.
        """
        return self.decode8() == 1

    def decodeDoubleArray(self, length: int) -> list[float]:
        """
        * Returns a decoded array of floats from the packet.
        """
        ret = []
        for _ in range(length):
            ret.append(self.decodeDouble())
        return ret

    def decodeShortList(self) -> list[int]:
        """
        * Returns a decoded array of shorts from the packet.
        """
        length = self.decode8()
        ret = []
        for _ in range(length):
            ret.append(self.decode16())
        return ret

    def decodeTransform(self) -> Transform3d:
        """
        * Returns a decoded Transform3d
        *
        * @return A decoded Tansform3d from the packet.
        """
        x = self.decodeDouble()
        y = self.decodeDouble()
        z = self.decodeDouble()
        translation = Translation3d(x, y, z)

        w = self.decodeDouble()
        x = self.decodeDouble()
        y = self.decodeDouble()
        z = self.decodeDouble()
        rotation = Rotation3d(Quaternion(w, x, y, z))

        return Transform3d(translation, rotation)

    def decodeList(self, serde: Serde[T]) -> list[T]:
        retList = []
        arr_len = self.decode8()
        for _ in range(arr_len):
            retList.append(serde.unpack(self))
        return retList

    def decodeOptional(self, serde: Serde[T]) -> Optional[T]:
        if self.decodeBoolean():
            return serde.unpack(self)
        else:
            return None

    def _encodeGeneric(self, packFormat, value):
        """
        Append bytes to the packet data buffer.
        """
        self.packetData = self.packetData + struct.pack(packFormat, value)
        self.size = len(self.packetData)

    def encode8(self, value: int):
        """
        Encodes a single byte and appends it to the packet.
        """
        self._encodeGeneric("<b", value)

    def encode16(self, value: int):
        """
        Encodes a short (2 bytes) and appends it to the packet.
        """
        self._encodeGeneric("<h", value)

    def encodeInt(self, value: int):
        """
        Encodes an int (4 bytes) and appends it to the packet.
        """
        self._encodeGeneric("<l", value)

    def encodeFloat(self, value: float):
        """
        Encodes a float (4 bytes) and appends it to the packet.
        """
        self._encodeGeneric("<f", value)

    def encodeLong(self, value: int):
        """
        Encodes a long (8 bytes) and appends it to the packet.
        """
        self._encodeGeneric("<q", value)

    def encodeDouble(self, value: float):
        """
        Encodes a double (8 bytes) and appends it to the packet.
        """
        self._encodeGeneric("<d", value)

    def encodeBoolean(self, value: bool):
        """
        Encodes a boolean as a single byte and appends it to the packet.
        """
        self.encode8(1 if value else 0)

    def encodeDoubleArray(self, values: list[float]):
        """
        Encodes an array of doubles and appends it to the packet.
        """
        self.encode8(len(values))
        for value in values:
            self.encodeDouble(value)

    def encodeShortList(self, values: list[int]):
        """
        Encodes a list of shorts, with length prefixed as a single byte.
        """
        self.encode8(len(values))
        for value in values:
            self.encode16(value)

    def encodeTransform(self, transform: Transform3d):
        """
        Encodes a Transform3d (translation and rotation) and appends it to the packet.
        """
        # Encode Translation3d part (x, y, z)
        self.encodeDouble(transform.translation().x)
        self.encodeDouble(transform.translation().y)
        self.encodeDouble(transform.translation().z)

        # Encode Rotation3d as Quaternion (w, x, y, z)
        quaternion = transform.rotation().getQuaternion()
        self.encodeDouble(quaternion.W())
        self.encodeDouble(quaternion.X())
        self.encodeDouble(quaternion.Y())
        self.encodeDouble(quaternion.Z())

    def encodeList(self, values: list[T], serde: Serde[T]):
        """
        Encodes a list of items using a specific serializer and appends it to the packet.
        """
        self.encode8(len(values))
        for item in values:
            packed = serde.pack(item)
            self.packetData = self.packetData + packed.getData()
            self.size = len(self.packetData)

    def encodeOptional(self, value: Optional[T], serde: Serde[T]):
        """
        Encodes an optional value using a specific serializer.
        """
        if value is None:
            self.encodeBoolean(False)
        else:
            self.encodeBoolean(True)
            packed = serde.pack(value)
            self.packetData = self.packetData + packed.getData()
            self.size = len(self.packetData)

    def encodeBytes(self, value: bytes):
        self.packetData = self.packetData + value
        self.size = len(self.packetData)
