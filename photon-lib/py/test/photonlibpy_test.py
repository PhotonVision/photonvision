from time import sleep
from photonlibpy import PhotonCamera
import ntcore
from photonlibpy.photonCamera import setVersionCheckEnabled


def test_roundTrip():

    ntcore.NetworkTableInstance.getDefault().stopServer()
    ntcore.NetworkTableInstance.getDefault().setServer("localhost")
    ntcore.NetworkTableInstance.getDefault().startClient4("meme")

    camera = PhotonCamera("WPI2024")

    setVersionCheckEnabled(False)

    for i in range(5):
        sleep(0.1)
        result = camera.getLatestResult()
        print(result)
        print(camera._rawBytesEntry.getTopic().getProperties())


if __name__ == "__main__":
    test_roundTrip()
