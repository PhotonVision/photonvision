from photonlibpy.packet import Packet
from photonlibpy.photonPipelineResult import PhotonPipelineResult
from data import rawBytes1
from data import rawBytes2
from data import rawBytes3
from data import rawBytes4
from data import rawBytes5
from data import rawBytes6


def setupCommon(bytesIn):
    res = PhotonPipelineResult()
    packet = Packet(bytesIn)
    res.populateFromPacket(packet)
    assert packet.outOfBytes is False
    return res


def test_byteParse1():
    res = setupCommon(rawBytes1)
    assert len(res.getTargets()) == 0


def test_byteParse2():
    res = setupCommon(rawBytes2)
    assert len(res.getTargets()) == 0


def test_byteParse3():
    res = setupCommon(rawBytes3)
    assert len(res.getTargets()) == 0


def test_byteParse4():
    res = setupCommon(rawBytes4)
    assert len(res.getTargets()) == 1


def test_byteParse5():
    res = setupCommon(rawBytes5)
    assert len(res.getTargets()) == 1


def test_byteParse6():
    res = setupCommon(rawBytes6)
    assert len(res.getTargets()) > 6
