import struct
from wpimath.geometry import Transform3d, Translation3d, Rotation3d, Quaternion
import wpilib

class Packet:

    """
     * Constructs an empty packet.
     *
     * @param self.size The self.size of the packet buffer.
    """
    def __init__(self, data:list[int]):
        self.packetData = data
        self.size = len(data)
        self.readPos = 0
        self.outOfBytes = False
    

    """ Clears the packet and resets the read and write positions."""
    def clear(self): 
        self.packetData = [0]*self.size
        self.readPos = 0
        self.outOfBytes = False
    

    def getSize(self): 
        return self.size
    
    _NO_MORE_BYTES_MESSAGE = """
    Photonlib - Ran out of bytes while decoding. 
    Make sure the version of photonvision on the coprocessor 
    matches the version of photonlib running in the robot code.
    """    

    def _getNextByte(self) -> int:
        retVal = 0x00
        
        if(not self.outOfBytes):
            try:
                retVal = 0x00ff & self.packetData[self.readPos]
                self.readPos += 1
            except IndexError:
                wpilib.reportError(Packet._NO_MORE_BYTES_MESSAGE, True)
                self.outOfBytes = True

        return retVal

    """
     * Returns the packet data. 
     *
     * @return The packet data.
    """
    def getData(self) -> list[int]: 
        return self.packetData
    

    """
     * Sets the packet data.
     *
     * @param data The packet data.
    """
    def setData(self, data:list[int]):
        self.clear()
        self.packetData = data
        self.size = len(self.packetData)
    
    def _decodeGeneric(self, unpackFormat, numBytes):

        # Read ints in from the data buffer
        intList = []
        for _ in range(numBytes):
            intList.append(self._getNextByte())
       
        # Interpret the bytes as a floating point number
        value = struct.unpack(unpackFormat, bytes(intList))[0]

        return value


    """
     * Returns a single decoded byte from the packet.
     *
     * @return A decoded byte from the packet.
    """
    def decode8(self) -> int: 
        return self._decodeGeneric(">b", 1)
    
    """
     * Returns a single decoded byte from the packet.
     *
     * @return A decoded byte from the packet.
    """
    def decode16(self) -> int: 
        return self._decodeGeneric(">h", 2)


    """
     * Returns a decoded int (32 bytes) from the packet.
     *
     * @return A decoded int from the packet.
    """
    def decode32(self) -> int: 
        return self._decodeGeneric(">l", 4)

    """
     * Returns a decoded double from the packet.
     *
     * @return A decoded double from the packet.
    """
    def decodeDouble(self) -> float: 
        return self._decodeGeneric(">d", 8)
    
    """
     * Returns a decoded boolean from the packet.
     *
     * @return A decoded boolean from the packet.
    """
    def decodeBoolean(self) -> bool:
        return (self.decode8() == 1)

    def decodeDoubleArray(self, length:int) -> list[float]:
        ret = []
        for _ in range(length):
            ret.append(self.decodeDouble())
        return ret
    
    def decodeTransform(self) -> Transform3d:
        x = self.decodeDouble()
        y = self.decodeDouble()
        z = self.decodeDouble()
        translation = Translation3d(x,y,z)

        w = self.decodeDouble()
        x = self.decodeDouble()
        y = self.decodeDouble()
        z = self.decodeDouble()
        rotation = Rotation3d(Quaternion(w,x,y,z))

        return Transform3d(translation, rotation)

