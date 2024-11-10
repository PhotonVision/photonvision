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

from time import sleep

import ntcore
from photonlibpy import PhotonCamera
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
