import pytest
from photonlibpy import Packet
from photonlibpy.targeting import PhotonPipelineResult


@pytest.fixture(autouse=True)
def setupCommon() -> None:
    pass


def test_Empty() -> None:
    packet = Packet(b"1")
    PhotonPipelineResult()
    packet.setData(bytes(0))
    PhotonPipelineResult.photonStruct.unpack(packet)
    # There is no need for an assert as we are checking
    # if this throws an exception (it should not)


@pytest.mark.parametrize(
    "robotStart, coprocStart, robotRestart, coprocRestart",
    [
        [1, 10, 30, 30],
        [10, 2, 30, 30],
        [10, 10, 30, 30],
        # Reboot just the robot
        [1, 1, 10, 30],
        # Reboot just the coproc
        [1, 1, 30, 10],
    ],
)
def test_RestartingRobotandCoproc(
    robotStart: int, coprocStart: int, robotRestart: int, coprocRestart: int
):
    # Python doesn't have a TimeSyncClient so we can't run this yet
    pass
