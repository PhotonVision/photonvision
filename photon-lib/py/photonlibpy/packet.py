import struct
from wpimath.geometry import Transform3d, Translation3d, Rotation3d, Quaternion
import wpilib


class Packet:
    def __init__(self, data: bytes):
        """
        * Constructs an empty packet.
        *
        * @param self.size The self.size of the packet buffer.
        """
        self.packetData = data
        self.size = len(data)
        self.readPos = 0
        self.outOfBytes = False

    def clear(self):
        """Clears the packet and resets the read and write positions."""
        self.packetData = [0] * self.size
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

        # Interpret the bytes as a floating point number
        value = struct.unpack(unpackFormat, bytes(intList))[0]

        return value

    def decode8(self) -> int:
        """
        * Returns a single decoded byte from the packet.
        *
        * @return A decoded byte from the packet.
        """
        return self._decodeGeneric(">b", 1)

    def decode16(self) -> int:
        """
        * Returns a single decoded byte from the packet.
        *
        * @return A decoded byte from the packet.
        """
        return self._decodeGeneric(">h", 2)

    def decode32(self) -> int:
        """
        * Returns a decoded int (32 bytes) from the packet.
        *
        * @return A decoded int from the packet.
        """
        return self._decodeGeneric(">l", 4)

    def decodeDouble(self) -> float:
        """
        * Returns a decoded double from the packet.
        *
        * @return A decoded double from the packet.
        """
        return self._decodeGeneric(">d", 8)

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
        *
        * @return A decoded array of floats from the packet.
        """
        ret = []
        for _ in range(length):
            ret.append(self.decodeDouble())
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
